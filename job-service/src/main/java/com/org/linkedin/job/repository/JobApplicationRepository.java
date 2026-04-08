package com.org.linkedin.job.repository;

import com.org.linkedin.domain.job.JobApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
  List<JobApplication> findByJobIdOrderByCreatedDateDesc(UUID jobId);

  List<JobApplication> findByJobIdAndStatusOrderByCreatedDateDesc(UUID jobId, String status);

  List<JobApplication> findByApplicantIdOrderByCreatedDateDesc(UUID applicantId);

  Optional<JobApplication> findByJobIdAndApplicantId(UUID jobId, UUID applicantId);
}
