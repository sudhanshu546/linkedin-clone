package com.org.linkedin.job.repository;

import com.org.linkedin.domain.job.JobApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
  List<JobApplication> findByJobIdOrderByCreatedAtDesc(UUID jobId);

  List<JobApplication> findByApplicantIdOrderByCreatedAtDesc(UUID applicantId);

  Optional<JobApplication> findByJobIdAndApplicantId(UUID jobId, UUID applicantId);
}
