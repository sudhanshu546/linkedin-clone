package com.org.linkedin.domain.job;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "jobs",
    indexes = {
        @Index(name = "idx_jobs_posted_by", columnList = "posted_by"),
        @Index(name = "idx_jobs_location_type", columnList = "location, job_type")
    })
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Job extends AbstractAuditingEntity<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private String company;

  @Column(nullable = false)
  private String location;

  @Column(name = "job_type")
  private String jobType; // FULL_TIME, PART_TIME, etc.

  @Column(name = "posted_by", nullable = false)
  private UUID postedBy; // Internal User ID
}
