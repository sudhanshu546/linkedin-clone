package com.org.linkedin.job.service;

import com.org.linkedin.dto.job.JobApplicationDTO;
import com.org.linkedin.dto.job.JobDTO;
import com.org.linkedin.utility.service.AdvanceSearchCriteria;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface JobService {
  JobDTO createJob(Authentication authentication, JobDTO jobDTO);

  Page<JobDTO> getAllJobs(Pageable pageable);

  JobDTO getJobById(UUID jobId);

  JobDTO updateJob(Authentication authentication, UUID jobId, JobDTO jobDTO);

  void deleteJob(Authentication authentication, UUID jobId);

  Page<JobDTO> searchJobs(
      String query,
      String title,
      String company,
      String location,
      String jobType,
      Pageable pageable);

  Page<JobDTO> advancedSearch(AdvanceSearchCriteria criteria);

  JobApplicationDTO applyToJob(
      Authentication authentication, UUID jobId, String resumeUrl, String coverLetter);

  List<JobApplicationDTO> getMyApplications(Authentication authentication);

  List<JobDTO> getMyPostings(Authentication authentication);

  List<JobApplicationDTO> getJobApplicants(UUID jobId, String status);

  JobApplicationDTO updateApplicationStatus(
      Authentication authentication,
      UUID jobId,
      UUID applicantId,
      String status,
      String interviewDate,
      String interviewLink);
}
