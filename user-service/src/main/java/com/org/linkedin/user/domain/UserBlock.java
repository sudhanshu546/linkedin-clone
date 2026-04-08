package com.org.linkedin.user.domain;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_blocks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBlock extends AbstractAuditingEntity<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "blocker_id", nullable = false)
  private UUID blockerId;

  @Column(name = "blocked_id", nullable = false)
  private UUID blockedId;
}
