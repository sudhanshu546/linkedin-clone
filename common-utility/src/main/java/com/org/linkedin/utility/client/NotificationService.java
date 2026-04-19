package com.org.linkedin.utility.client;

import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.notification.NotificationDTO;
import com.org.linkedin.dto.notification.NotificationTargetDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "notification-service",
    path = "/ns",
    fallback = NotificationServiceFallback.class)
public interface NotificationService {
  @GetMapping("/notification/{key}")
  public ResponseEntity<ApiResponse<NotificationDTO>> getNotificationByKey(
      @PathVariable("key") String key);

  @PostMapping("/notification-target")
  public ResponseEntity<ApiResponse<NotificationTargetDTO>> saveNotificationTarget(
      @RequestBody NotificationTargetDTO notificationTargetDTO);
}
