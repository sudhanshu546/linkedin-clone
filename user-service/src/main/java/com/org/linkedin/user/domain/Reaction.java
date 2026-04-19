package com.org.linkedin.user.domain;

import com.org.linkedin.domain.AbstractAuditingEntity;
import com.org.linkedin.domain.enumeration.ReactionType;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(
    name = "post_reactions",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"post_id", "user_id"})},
    indexes = {
      @Index(name = "idx_reaction_post_id", columnList = "post_id"),
      @Index(name = "idx_reaction_post_user", columnList = "post_id, user_id")
    })
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE post_reactions SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false OR is_deleted IS NULL")
public class Reaction extends AbstractAuditingEntity<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @Column(name = "post_id", insertable = false, updatable = false)
  private UUID postId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private ReactionType type;
}
