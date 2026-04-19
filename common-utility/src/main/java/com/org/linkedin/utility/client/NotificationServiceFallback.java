package com.org.linkedin.utility.client;

import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.notification.NotificationDTO;
import com.org.linkedin.dto.notification.NotificationTargetDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationServiceFallback implements NotificationService {

  @Override
  public ResponseEntity<ApiResponse<NotificationDTO>> getNotificationByKey(String key) {
    return ResponseEntity.ok(
        ApiResponse.<NotificationDTO>builder()
            .status("partial")
            .message("Notification service unavailable.")
            .build());
  }

  @Override
  public ResponseEntity<ApiResponse<NotificationTargetDTO>> saveNotificationTarget(
      NotificationTargetDTO notificationTargetDTO) {
    return ResponseEntity.ok(
        ApiResponse.<NotificationTargetDTO>builder()
            .status("partial")
            .message("Notification service unavailable. Target not saved.")
            .build());
  }
}
