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
@Table(
    name = "profile_skills",
    uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "skill_id"}))
@SQLDelete(sql = "UPDATE profile_skills SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSkill extends AbstractAuditingEntity<UUID> implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "profile_id", nullable = false)
  @JsonBackReference
  private Profile profile;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_id", nullable = false)
  private Skill skill;

  @Builder.Default private Integer endorsementCount = 0;
}
