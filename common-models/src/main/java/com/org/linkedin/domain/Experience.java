package com.org.linkedin.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "experience")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE experience SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Experience extends AbstractAuditingEntity<UUID> implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "profile_id", nullable = false)
  @com.fasterxml.jackson.annotation.JsonBackReference
  private Profile profile;

  private String position;
  private String company;
  private LocalDate startDate;
  private LocalDate endDate;
  private boolean isCurrentlyWorking;

  @Column(length = 2000)
  private String description;
}
