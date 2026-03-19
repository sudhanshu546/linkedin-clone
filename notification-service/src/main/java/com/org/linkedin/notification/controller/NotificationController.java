package com.org.linkedin.notification.controller;

import com.org.linkedin.dto.BaseResponse;
import com.org.linkedin.dto.notification.NotificationDTO;
import com.org.linkedin.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${apiPrefix}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public BaseResponse<List<NotificationDTO>> getMyNotifications(Authentication authentication) {
        List<NotificationDTO> result = notificationService.getMyNotifications(authentication);
        return BaseResponse.<List<NotificationDTO>>builder()
                .status(HttpStatus.OK.value())
                .result(result)
                .build();
    }

    @GetMapping("/unread")
    public BaseResponse<List<NotificationDTO>> getMyUnreadNotifications(Authentication authentication) {
        List<NotificationDTO> result = notificationService.getMyUnreadNotifications(authentication);
        return BaseResponse.<List<NotificationDTO>>builder()
                .status(HttpStatus.OK.value())
                .result(result)
                .build();
    }

    @GetMapping("/unread/count")
    public BaseResponse<Long> getMyUnreadCount(Authentication authentication) {
        long count = notificationService.getMyUnreadCount(authentication);
        return BaseResponse.<Long>builder()
                .status(HttpStatus.OK.value())
                .result(count)
                .build();
    }

    @PutMapping("/{id}/read")
    public BaseResponse<Void> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return BaseResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Notification marked as read")
                .build();
    }

    @PatchMapping("/read-all")
    public BaseResponse<Void> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication);
        return BaseResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("All notifications marked as read")
                .build();
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> deleteNotification(@PathVariable UUID id) {
        notificationService.deleteNotification(id);
        return BaseResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Notification deleted")
                .build();
    }

    @DeleteMapping("/all")
    public BaseResponse<Void> deleteAllNotifications(Authentication authentication) {
        notificationService.deleteAllNotifications(authentication);
        return BaseResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("All notifications deleted")
                .build();
    }
}
