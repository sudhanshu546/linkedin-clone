package com.org.linkedin.job.service;

import com.org.linkedin.dto.BasePageResponse;
import com.org.linkedin.dto.job.JobApplicationDTO;
import com.org.linkedin.dto.job.JobDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface JobService {
  JobDTO createJob(Authentication authentication, JobDTO jobDTO);

  BasePageResponse<List<JobDTO>> getAllJobs(Pageable pageable);

  JobDTO getJobById(UUID jobId);

  JobDTO updateJob(UUID jobId, JobDTO jobDTO);

  void deleteJob(UUID jobId);

  BasePageResponse<List<JobDTO>> searchJobs(
      String query,
      String title,
      String company,
      String location,
      String jobType,
      Pageable pageable);

  JobApplicationDTO applyToJob(Authentication authentication, UUID jobId);

  List<JobApplicationDTO> getMyApplications(Authentication authentication);

  List<JobDTO> getMyPostings(Authentication authentication);

  List<JobApplicationDTO> getJobApplicants(UUID jobId);

  JobApplicationDTO updateApplicationStatus(UUID jobId, UUID applicantId, String status);
}
