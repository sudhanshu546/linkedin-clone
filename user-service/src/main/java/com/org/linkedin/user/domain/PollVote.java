package com.org.linkedin.user.domain;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "poll_votes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollVote extends AbstractAuditingEntity<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "post_id", nullable = false)
  private UUID postId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "option_id", nullable = false)
  private UUID optionId;
}
