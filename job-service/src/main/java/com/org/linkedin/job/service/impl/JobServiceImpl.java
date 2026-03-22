package com.org.linkedin.job.service.impl;

import com.org.linkedin.domain.job.Job;
import com.org.linkedin.domain.job.JobApplication;
import com.org.linkedin.dto.BasePageResponse;
import com.org.linkedin.dto.job.JobApplicationDTO;
import com.org.linkedin.dto.job.JobDTO;
import com.org.linkedin.job.mapper.JobApplicationMapper;
import com.org.linkedin.job.mapper.JobMapper;
import com.org.linkedin.job.repository.JobApplicationRepository;
import com.org.linkedin.job.repository.JobRepository;
import com.org.linkedin.job.service.JobService;
import com.org.linkedin.utility.client.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

  private final JobRepository jobRepository;
  private final JobApplicationRepository jobApplicationRepository;
  private final UserService userService;
  private final JobMapper jobMapper;
  private final JobApplicationMapper jobApplicationMapper;

  @Override
  public JobDTO createJob(Authentication authentication, JobDTO jobDTO) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();

    Job job = jobMapper.toEntity(jobDTO);
    job.setPostedBy(internalUserId);
    job = jobRepository.save(job);
    return jobMapper.toDto(job);
  }

  @Override
  public BasePageResponse<List<JobDTO>> getAllJobs(Pageable pageable) {
    Page<Job> jobPage = jobRepository.findAll(pageable);
    List<JobDTO> dtoList = jobMapper.toDto(jobPage.getContent());

    return BasePageResponse.<List<JobDTO>>builder()
        .status(HttpStatus.OK.value())
        .result(dtoList)
        .pageNumber(jobPage.getNumber())
        .pageSize(jobPage.getSize())
        .totalRecords(jobPage.getTotalElements())
        .build();
  }

  @Override
  public JobDTO getJobById(UUID jobId) {
    Job job =
        jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
    return jobMapper.toDto(job);
  }

  @Override
  public JobDTO updateJob(UUID jobId, JobDTO jobDTO) {
    Job job =
        jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
    jobMapper.partialUpdate(job, jobDTO);
    job = jobRepository.save(job);
    return jobMapper.toDto(job);
  }

  @Override
  public void deleteJob(UUID jobId) {
    jobRepository.deleteById(jobId);
  }

  @Override
  public BasePageResponse<List<JobDTO>> searchJobs(
      String query,
      String title,
      String company,
      String location,
      String jobType,
      Pageable pageable) {
    Page<Job> jobPage =
        jobRepository.searchJobs(query, title, company, location, jobType, pageable);
    List<JobDTO> dtoList = jobMapper.toDto(jobPage.getContent());

    return BasePageResponse.<List<JobDTO>>builder()
        .status(HttpStatus.OK.value())
        .result(dtoList)
        .pageNumber(jobPage.getNumber())
        .pageSize(jobPage.getSize())
        .totalRecords(jobPage.getTotalElements())
        .build();
  }

  @Override
  public JobApplicationDTO applyToJob(Authentication authentication, UUID jobId) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();

    JobApplication application =
        JobApplication.builder().jobId(jobId).applicantId(internalUserId).status("PENDING").build();

    application = jobApplicationRepository.save(application);
    return jobApplicationMapper.toDto(application);
  }

  @Override
  public List<JobApplicationDTO> getMyApplications(Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();
    List<JobApplication> apps =
        jobApplicationRepository.findByApplicantIdOrderByCreatedAtDesc(internalUserId);
    return jobApplicationMapper.toDto(apps);
  }

  @Override
  public List<JobDTO> getMyPostings(Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();
    List<Job> postings = jobRepository.findByPostedByOrderByCreatedAtDesc(internalUserId);
    return jobMapper.toDto(postings);
  }

  @Override
  public List<JobApplicationDTO> getJobApplicants(UUID jobId) {
    List<JobApplication> apps = jobApplicationRepository.findByJobIdOrderByCreatedAtDesc(jobId);
    return jobApplicationMapper.toDto(apps);
  }

  @Override
  public JobApplicationDTO updateApplicationStatus(UUID jobId, UUID applicantId, String status) {
    JobApplication application =
        jobApplicationRepository
            .findByJobIdAndApplicantId(jobId, applicantId)
            .orElseThrow(() -> new RuntimeException("Application not found"));
    application.setStatus(status);
    application = jobApplicationRepository.save(application);
    return jobApplicationMapper.toDto(application);
  }
}
