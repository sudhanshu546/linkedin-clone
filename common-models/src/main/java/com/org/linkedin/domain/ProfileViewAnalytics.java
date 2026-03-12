package com.org.linkedin.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "profile_view_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileViewAnalytics extends AbstractAuditingEntity<UUID> implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "profile_owner_id", nullable = false, unique = true)
  private UUID profileOwnerId;

  @Column(name = "total_views")
  private Long totalViews;

  @Column(name = "last_viewed_at")
  private LocalDateTime lastViewedAt;
}
