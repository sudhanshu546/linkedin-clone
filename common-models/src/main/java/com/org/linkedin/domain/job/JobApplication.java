package com.org.linkedin.domain.job;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "job_applications")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE job_applications SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false OR is_deleted IS NULL")
public class JobApplication extends AbstractAuditingEntity<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "job_id", nullable = false)
  private UUID jobId;

  @Column(name = "applicant_id", nullable = false)
  private UUID applicantId; // Internal User ID

  @Column(nullable = false)
  private String status; // PENDING, ACCEPTED, REJECTED

  @Column(name = "resume_url")
  private String resumeUrl;

  @Column(name = "cover_letter", length = 2000)
  private String coverLetter;

  @Column(name = "interview_date")
  private String interviewDate;

  @Column(name = "interview_link")
  private String interviewLink;
}
