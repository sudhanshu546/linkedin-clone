package com.org.linkedin.domain.job;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "job_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "job_id", nullable = false)
  private UUID jobId;

  @Column(name = "applicant_id", nullable = false)
  private UUID applicantId; // Internal User ID

  @Column(nullable = false)
  private String status; // PENDING, ACCEPTED, REJECTED

  @Column(name = "applied_at")
  private LocalDateTime appliedAt = LocalDateTime.now();
}
