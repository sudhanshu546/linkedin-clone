package com.org.linkedin.dto.notification;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
  private UUID id;
  private UUID recipientId;
  private UUID senderId;
  private String type;
  private String message;
  private boolean isRead;
  private Long createdDate;
}
