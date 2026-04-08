package com.org.linkedin.notification.service.impl;

import com.org.linkedin.dto.notification.NotificationDTO;
import com.org.linkedin.notification.domain.Notification;
import com.org.linkedin.notification.mapper.NotificationMapper;
import com.org.linkedin.notification.repo.NotificationRepository;
import com.org.linkedin.notification.service.NotificationService;
import com.org.linkedin.utility.client.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserService userService;
  private final NotificationMapper notificationMapper;

  @Override
  public List<NotificationDTO> getMyNotifications(Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();
    List<Notification> notifications =
        notificationRepository.findByRecipientIdOrderByCreatedDateDesc(internalUserId);
    return notificationMapper.toDto(notifications);
  }

  @Override
  public List<NotificationDTO> getMyUnreadNotifications(Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();
    List<Notification> notifications =
        notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedDateDesc(
            internalUserId);
    return notificationMapper.toDto(notifications);
  }

  @Override
  public long getMyUnreadCount(Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();
    return notificationRepository.countByRecipientIdAndIsReadFalse(internalUserId);
  }

  @Override
  public void markAsRead(UUID id) {
    notificationRepository
        .findById(id)
        .ifPresent(
            n -> {
              n.setRead(true);
              notificationRepository.save(n);
            });
  }

  @Override
  @Transactional
  public void markAllAsRead(Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();
    notificationRepository
        .findByRecipientIdAndIsReadFalseOrderByCreatedDateDesc(internalUserId)
        .forEach(
            n -> {
              n.setRead(true);
              notificationRepository.save(n);
            });
  }

  @Override
  public void deleteNotification(UUID id) {
    notificationRepository.deleteById(id);
  }

  @Override
  @Transactional
  public void deleteAllNotifications(Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();
    notificationRepository.deleteByRecipientId(internalUserId);
  }
}
