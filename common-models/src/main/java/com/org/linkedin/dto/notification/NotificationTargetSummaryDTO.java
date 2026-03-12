package com.org.linkedin.dto.notification;

import java.util.UUID;
import lombok.Data;

@Data
public class NotificationTargetSummaryDTO {

  private UUID targetId;
  private Integer readStatus;
  private Long notificationCount;

  public NotificationTargetSummaryDTO(UUID targetId, Integer readStatus, Long notificationCount) {
    this.targetId = targetId;
    this.readStatus = readStatus;
    this.notificationCount = notificationCount;
  }
}
