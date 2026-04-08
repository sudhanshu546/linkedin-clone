package com.org.linkedin.user.domain;

import com.org.linkedin.domain.enumeration.ReactionType;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "post_reactions",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"post_id", "user_id"})},
    indexes = {
        @Index(name = "idx_reaction_post_id", columnList = "post_id"),
        @Index(name = "idx_reaction_post_user", columnList = "post_id, user_id")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reaction {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "post_id", nullable = false)
  private UUID postId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private ReactionType type;
}
