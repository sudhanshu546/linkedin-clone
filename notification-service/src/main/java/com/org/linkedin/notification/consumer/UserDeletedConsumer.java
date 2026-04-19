package com.org.linkedin.notification.consumer;

import com.org.linkedin.dto.event.UserDeletedEvent;
import com.org.linkedin.notification.domain.Notification;
import com.org.linkedin.notification.repo.NotificationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDeletedConsumer {

  private final NotificationRepository notificationRepository;

  @KafkaListener(topics = "${kafka.topics.user-deleted}", groupId = "${spring.kafka.consumer.group-id}")
  @Transactional
  public void consumeUserDeleted(@Payload UserDeletedEvent event) {
    log.info("Received UserDeletedEvent for userId: {}", event.getUserId());
    try {
      UUID userId = UUID.fromString(event.getUserId());

      // Soft delete notifications where user is recipient
      List<Notification> received = notificationRepository.findByRecipientId(userId);
      for (Notification n : received) {
        n.setIsDeleted(true);
      }
      notificationRepository.saveAll(received);

      // Soft delete notifications where user is sender
      List<Notification> sent = notificationRepository.findBySenderId(userId);
      for (Notification n : sent) {
        n.setIsDeleted(true);
      }
      notificationRepository.saveAll(sent);

      log.info("Soft-deleted notifications for userId: {}", userId);
    } catch (Exception e) {
      log.error("Error processing UserDeletedEvent in notification-service: {}", e.getMessage(), e);
    }
  }
}
