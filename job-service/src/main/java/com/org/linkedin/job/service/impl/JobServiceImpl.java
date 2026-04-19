package com.org.linkedin.job.service.impl;

import static com.org.linkedin.utility.ProjectConstants.*;

import com.org.linkedin.domain.job.Job;
import com.org.linkedin.domain.job.JobApplication;
import com.org.linkedin.dto.event.JobApplicationStatusUpdatedEvent;
import com.org.linkedin.dto.job.JobApplicationDTO;
import com.org.linkedin.dto.job.JobDTO;
import com.org.linkedin.job.mapper.JobApplicationMapper;
import com.org.linkedin.job.mapper.JobMapper;
import com.org.linkedin.job.repository.JobApplicationRepository;
import com.org.linkedin.job.repository.JobRepository;
import com.org.linkedin.job.service.JobService;
import com.org.linkedin.utility.client.UserService;
import com.org.linkedin.utility.service.AdvanceSearchCriteria;
import com.org.linkedin.utility.service.CommonUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Service implementation for managing job listings and applications. Integrates with Search Service
 * (via Kafka) for discovery and Uses Redis for caching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {

  private final JobRepository jobRepository;
  private final JobApplicationRepository jobApplicationRepository;
  private final UserService userService;
  private final JobMapper jobMapper;
  private final JobApplicationMapper jobApplicationMapper;

  private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

  @PersistenceContext private final EntityManager entityManager;
  private final CommonUtil commonUtil;

  /** Creates a new job posting. Synchronizes with Elasticsearch via Kafka. */
  @Override
  @CacheEvict(value = "jobs", allEntries = true)
  public JobDTO createJob(Authentication authentication, JobDTO jobDTO) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();

    Job job = jobMapper.toEntity(jobDTO);
    job.setPostedBy(internalUserId);
    job = jobRepository.save(job);
    JobDTO result = jobMapper.toDto(job);

    // Sync with Search Service
    syncJobToSearch(result, ACTION_CREATE);

    return result;
  }

  /** Retrieves all jobs with pagination. */
  @Override
  public Page<JobDTO> getAllJobs(Pageable pageable) {
    Page<Job> jobPage = jobRepository.findAll(pageable);
    List<JobDTO> dtoList = jobMapper.toDto(jobPage.getContent());
    return new PageImpl<>(dtoList, pageable, jobPage.getTotalElements());
  }

  /**
   * Fetches a specific job by its unique identifier. Uses Redis to cache job details for faster
   * access.
   */
  @Override
  @Cacheable(value = "jobs", key = "#jobId")
  public JobDTO getJobById(UUID jobId) {
    log.debug("Fetching job {} from database", jobId);
    Job job =
        jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException(ERROR_JOB_NOT_FOUND));
    return jobMapper.toDto(job);
  }

  /** Updates an existing job posting. Evicts cache to ensure consistency. */
  @Override
  @CacheEvict(value = "jobs", key = "#jobId")
  public JobDTO updateJob(Authentication authentication, UUID jobId, JobDTO jobDTO) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();

    Job job =
        jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException(ERROR_JOB_NOT_FOUND));

    if (!job.getPostedBy().equals(internalUserId)) {
      throw new RuntimeException("Unauthorized to update this job");
    }

    jobMapper.partialUpdate(job, jobDTO);
    job = jobRepository.save(job);
    JobDTO result = jobMapper.toDto(job);

    // Sync with Search Service
    syncJobToSearch(result, ACTION_UPDATE);

    return result;
  }

  /** Deletes a job posting. */
  @Override
  @CacheEvict(value = "jobs", key = "#jobId")
  public void deleteJob(Authentication authentication, UUID jobId) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();

    Job job =
        jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException(ERROR_JOB_NOT_FOUND));

    if (!job.getPostedBy().equals(internalUserId)) {
      throw new RuntimeException("Unauthorized to delete this job");
    }

    syncJobToSearch(JobDTO.builder().id(jobId).build(), ACTION_DELETE);
    jobRepository.deleteById(jobId);
  }

  /** Basic search functionality for jobs. */
  @Override
  public Page<JobDTO> searchJobs(
      String query,
      String title,
      String company,
      String location,
      String jobType,
      Pageable pageable) {
    Page<Job> jobPage =
        jobRepository.searchJobs(query, title, company, location, jobType, pageable);
    List<JobDTO> dtoList = jobMapper.toDto(jobPage.getContent());

    return new PageImpl<>(dtoList, pageable, jobPage.getTotalElements());
  }

  /** Allows a user to apply for a job. */
  @Override
  public JobApplicationDTO applyToJob(
      Authentication authentication, UUID jobId, String resumeUrl, String coverLetter) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();

    JobApplication application =
        JobApplication.builder()
            .jobId(jobId)
            .applicantId(internalUserId)
            .status(STATUS_PENDING)
            .resumeUrl(resumeUrl)
            .coverLetter(coverLetter)
            .build();

    application = jobApplicationRepository.save(application);
    return jobApplicationMapper.toDto(application);
  }

  /** Retrieves applications submitted by the current authenticated user. */
  @Override
  public List<JobApplicationDTO> getMyApplications(Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();
    List<JobApplication> apps =
        jobApplicationRepository.findByApplicantIdOrderByCreatedDateDesc(internalUserId);
    return jobApplicationMapper.toDto(apps);
  }

  /** Retrieves job postings created by the current authenticated user. */
  @Override
  public List<JobDTO> getMyPostings(Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserById(keycloakId).getBody().getData().getId();
    List<Job> postings = jobRepository.findByPostedByOrderByCreatedDateDesc(internalUserId);
    return jobMapper.toDto(postings);
  }

  /** Retrieves all applicants for a specific job, optionally filtered by status. */
  @Override
  public List<JobApplicationDTO> getJobApplicants(UUID jobId, String status) {
    List<JobApplication> apps;
    if (status != null && !status.isEmpty()) {
      apps = jobApplicationRepository.findByJobIdAndStatusOrderByCreatedDateDesc(jobId, status);
    } else {
      apps = jobApplicationRepository.findByJobIdOrderByCreatedDateDesc(jobId);
    }
    return jobApplicationMapper.toDto(apps);
  }

  /** Advanced recruiter search for job listings. */
  @Override
  public Page<JobDTO> advancedSearch(AdvanceSearchCriteria criteria) {
    log.debug("Entering advancedSearch for Jobs");

    Pageable pageable;
    if (criteria == null) {
      pageable = PageRequest.of(0, 20);
      criteria = new AdvanceSearchCriteria();
      criteria.setFilters(new ArrayList<>());
    } else {
      pageable = PageRequest.of(criteria.getPageNumber(), criteria.getPageSize());
      if (criteria.getFilters() == null) {
        criteria.setFilters(new ArrayList<>());
      }
    }

    List<AdvanceSearchCriteria.Filter> filters = criteria.getFilters();
    commonUtil.addIsEnabledFilter(filters);

    CriteriaQuery<Job> criteriaQuery =
        (CriteriaQuery<Job>) commonUtil.getJpaQuery(filters, Job.class);

    Root<Job> root = (Root<Job>) criteriaQuery.getRoots().iterator().next();
    criteriaQuery.orderBy(entityManager.getCriteriaBuilder().desc(root.get("createdDate")));

    List<Job> jobList =
        entityManager
            .createQuery(criteriaQuery)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

    long totalCount = commonUtil.getTotalCount(Job.class, filters);
    List<JobDTO> dtoList = jobMapper.toDto(jobList);

    return new PageImpl<>(dtoList, pageable, totalCount);
  }

  /**
   * Updates the status of a job application (e.g., SCREENING, INTERVIEW, REJECTED). Triggers a
   * notification to the applicant.
   */
  @Override
  public JobApplicationDTO updateApplicationStatus(
      Authentication authentication,
      UUID jobId,
      UUID applicantId,
      String status,
      String interviewDate,
      String interviewLink) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();

    Job job =
        jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException(ERROR_JOB_NOT_FOUND));

    if (!job.getPostedBy().equals(internalUserId)) {
      throw new RuntimeException("Unauthorized to update application status for this job");
    }

    JobApplication application =
        jobApplicationRepository
            .findByJobIdAndApplicantId(jobId, applicantId)
            .orElseThrow(() -> new RuntimeException(ERROR_APP_NOT_FOUND));
    application.setStatus(status);

    if (interviewDate != null) {
      application.setInterviewDate(interviewDate);
    }
    if (interviewLink != null) {
      application.setInterviewLink(interviewLink);
    }

    application = jobApplicationRepository.save(application);

    // Notify the user via Notification Service
    notifyApplicantStatusChange(jobId, applicantId, status);

    return jobApplicationMapper.toDto(application);
  }

  private void syncJobToSearch(JobDTO job, String action) {
    kafkaTemplate.send(
        TOPIC_JOB_UPDATED,
        job.getId().toString(),
        com.org.linkedin.dto.event.JobUpdatedEvent.builder().job(job).action(action).build());
  }

  private void notifyApplicantStatusChange(UUID jobId, UUID applicantId, String status) {
    kafkaTemplate.send(
        TOPIC_JOB_APP_STATUS_UPDATED,
        applicantId.toString(),
        JobApplicationStatusUpdatedEvent.builder()
            .jobId(jobId)
            .applicantId(applicantId)
            .status(status)
            .jobTitle(jobRepository.findById(jobId).map(Job::getTitle).orElse(DEFAULT_JOB_TITLE))
            .timestamp(System.currentTimeMillis())
            .build());
  }
}
