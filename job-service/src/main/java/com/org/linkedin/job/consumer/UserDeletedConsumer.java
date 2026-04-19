package com.org.linkedin.job.consumer;

import com.org.linkedin.domain.job.Job;
import com.org.linkedin.domain.job.JobApplication;
import com.org.linkedin.dto.event.UserDeletedEvent;
import com.org.linkedin.job.repository.JobApplicationRepository;
import com.org.linkedin.job.repository.JobRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDeletedConsumer {

  private final JobRepository jobRepository;
  private final JobApplicationRepository jobApplicationRepository;

  @KafkaListener(
      topics = "${kafka.topics.user-deleted}",
      groupId = "${spring.kafka.consumer.group-id}")
  @Transactional
  public void consumeUserDeleted(@Payload UserDeletedEvent event) {
    log.info("Received UserDeletedEvent for userId: {}", event.getUserId());
    try {
      UUID userId = UUID.fromString(event.getUserId());

      // Soft delete jobs posted by the user
      List<Job> jobs = jobRepository.findByPostedByOrderByCreatedDateDesc(userId);
      if (!jobs.isEmpty()) {
        for (Job job : jobs) {
          job.setIsDeleted(true);
          // Also soft delete applications for these jobs
          List<JobApplication> appsForJob =
              jobApplicationRepository.findByJobIdOrderByCreatedDateDesc(job.getId());
          for (JobApplication app : appsForJob) {
            app.setIsDeleted(true);
          }
          jobApplicationRepository.saveAll(appsForJob);
        }
        jobRepository.saveAll(jobs);
        log.info("Soft-deleted {} jobs and their applications for userId: {}", jobs.size(), userId);
      }

      // Soft delete applications submitted by the user
      List<JobApplication> userApps =
          jobApplicationRepository.findByApplicantIdOrderByCreatedDateDesc(userId);
      if (!userApps.isEmpty()) {
        for (JobApplication app : userApps) {
          app.setIsDeleted(true);
        }
        jobApplicationRepository.saveAll(userApps);
        log.info("Soft-deleted {} applications submitted by userId: {}", userApps.size(), userId);
      }

    } catch (Exception e) {
      log.error("Error processing UserDeletedEvent in job-service: {}", e.getMessage(), e);
    }
  }
}
