package com.org.linkedin.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "recommendations")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE recommendations SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Recommendation extends AbstractAuditingEntity<UUID> implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "profile_id", nullable = false)
  @JsonBackReference
  private Profile profile;

  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @Column(length = 2000, nullable = false)
  private String content;

  private String relationship;

  @Column(nullable = false)
  @Builder.Default
  private String status = "PENDING"; // PENDING, ACCEPTED, HIDDEN
}
