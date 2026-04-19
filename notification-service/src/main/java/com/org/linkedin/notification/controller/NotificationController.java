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

/**
 * Controller for managing user notifications.
 * Handles retrieval of alerts for likes, comments, connections, and system updates.
 */
@RestController
@RequestMapping("${apiPrefix}/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  /**
   * Retrieves all notifications for the current authenticated user.
   *
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with a list of all NotificationDTOs for the user.
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<NotificationDTO>>> getMyNotifications(
      Authentication authentication) {
    List<NotificationDTO> result = notificationService.getMyNotifications(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Retrieves only unread notifications for the current authenticated user.
   *
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with a list of unread NotificationDTOs for the user.
   */
  @GetMapping("/unread")
  public ResponseEntity<ApiResponse<List<NotificationDTO>>> getMyUnreadNotifications(
      Authentication authentication) {
    List<NotificationDTO> result = notificationService.getMyUnreadNotifications(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Gets the total count of unread notifications for the user badge display.
   *
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with the total count of unread notifications.
   */
  @GetMapping("/unread/count")
  public ResponseEntity<ApiResponse<Long>> getMyUnreadCount(Authentication authentication) {
    long count = notificationService.getMyUnreadCount(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", count));
  }

  /**
   * Marks a specific notification as read.
   *
   * @param id The unique identifier of the notification to mark as read.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @PutMapping("/{id}/read")
  public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
    notificationService.markAsRead(id);
    return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
  }

  /**
   * Marks all of the user's notifications as read in a single operation.
   *
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @PatchMapping("/read-all")
  public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
    notificationService.markAllAsRead(authentication);
    return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
  }

  /**
   * Permanently deletes a single notification.
   *
   * @param id The unique identifier of the notification to delete.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable UUID id) {
    notificationService.deleteNotification(id);
    return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
  }

  /**
   * Wipes all notifications for the current user.
   *
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @DeleteMapping("/all")
  public ResponseEntity<ApiResponse<Void>> deleteAllNotifications(Authentication authentication) {
    notificationService.deleteAllNotifications(authentication);
    return ResponseEntity.ok(ApiResponse.success("All notifications deleted", null));
  }
}
