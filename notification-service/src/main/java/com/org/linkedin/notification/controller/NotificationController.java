package com.org.linkedin.notification.controller;

import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.notification.NotificationDTO;
import com.org.linkedin.notification.service.NotificationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${apiPrefix}/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<NotificationDTO>>> getMyNotifications(
      Authentication authentication) {
    List<NotificationDTO> result = notificationService.getMyNotifications(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @GetMapping("/unread")
  public ResponseEntity<ApiResponse<List<NotificationDTO>>> getMyUnreadNotifications(
      Authentication authentication) {
    List<NotificationDTO> result = notificationService.getMyUnreadNotifications(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @GetMapping("/unread/count")
  public ResponseEntity<ApiResponse<Long>> getMyUnreadCount(Authentication authentication) {
    long count = notificationService.getMyUnreadCount(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", count));
  }

  @PutMapping("/{id}/read")
  public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
    notificationService.markAsRead(id);
    return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
  }

  @PatchMapping("/read-all")
  public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
    notificationService.markAllAsRead(authentication);
    return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable UUID id) {
    notificationService.deleteNotification(id);
    return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
  }

  @DeleteMapping("/all")
  public ResponseEntity<ApiResponse<Void>> deleteAllNotifications(Authentication authentication) {
    notificationService.deleteAllNotifications(authentication);
    return ResponseEntity.ok(ApiResponse.success("All notifications deleted", null));
  }
}
