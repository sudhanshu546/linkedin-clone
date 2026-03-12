package com.org.linkedin.domain.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_target_count_summary")
@Getter
@Setter
public class NotificationTargetSummary {
  @Id
  @Column(name = "target_id")
  private UUID targetId;

  @Column(name = "read_status")
  private Integer readStatus;

  @Column(name = "notification_count")
  private Long notificationCount;
}
