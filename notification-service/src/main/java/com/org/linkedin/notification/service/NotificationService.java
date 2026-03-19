package com.org.linkedin.notification.service;

import com.org.linkedin.dto.notification.NotificationDTO;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    List<NotificationDTO> getMyNotifications(Authentication authentication);
    List<NotificationDTO> getMyUnreadNotifications(Authentication authentication);
    long getMyUnreadCount(Authentication authentication);
    void markAsRead(UUID id);
    void markAllAsRead(Authentication authentication);
    void deleteNotification(UUID id);
    void deleteAllNotifications(Authentication authentication);
}
