package com.org.linkedin.notification.consumer;

import com.org.linkedin.dto.event.*;
import com.org.linkedin.notification.domain.Notification;
import com.org.linkedin.notification.publisher.NotificationPublisher;
import com.org.linkedin.notification.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

  private final NotificationRepository notificationRepository;
  private final NotificationPublisher notificationPublisher;

  @Transactional
  @KafkaListener(
      topics = "${kafka.topics.connection-requested}",
      groupId = "notification-service-group")
  public void consumeConnectionRequest(@Payload ConnectionRequestedEvent event) {
    log.info("Received ConnectionRequestedEvent: {}", event);
    Notification notification =
        saveNotification(
            event.getReceiverId(),
            event.getSenderId(),
            "CONNECTION_REQUEST",
            "You have a new connection request.");
    notificationPublisher.pushNotification(notification);
  }

  @Transactional
  @KafkaListener(
      topics = "${kafka.topics.connection-accepted}",
      groupId = "notification-service-group")
  public void consumeConnectionAccepted(@Payload ConnectionAcceptedEvent event) {
    log.info("Received ConnectionAcceptedEvent: {}", event);
    Notification notification =
        saveNotification(
            event.getRequesterId(),
            event.getReceiverId(),
            "CONNECTION_ACCEPTED",
            "Your connection request was accepted.");
    notificationPublisher.pushNotification(notification);
  }

  @Transactional
  @KafkaListener(topics = "${kafka.topics.post-liked}", groupId = "notification-service-group")
  public void consumePostLiked(@Payload PostLikedEvent event) {
    log.info("Received PostLikedEvent: {}", event);
    if (event.getPostAuthorId() != null) {
      Notification notification =
          saveNotification(
              java.util.UUID.fromString(event.getPostAuthorId()),
              java.util.UUID.fromString(event.getUserId()),
              "POST_LIKED",
              event.getUserName() + " liked your post.");
      notificationPublisher.pushNotification(notification);
    }
  }

  @Transactional
  @KafkaListener(topics = "${kafka.topics.comment-created}", groupId = "notification-service-group")
  public void consumeCommentCreated(@Payload CommentCreatedEvent event) {
    log.info("Received CommentCreatedEvent: {}", event);
    if (event.getPostAuthorId() != null) {
      Notification notification =
          saveNotification(
              java.util.UUID.fromString(event.getPostAuthorId()),
              java.util.UUID.fromString(event.getUserId()),
              "COMMENT_CREATED",
              event.getUserName() + " commented on your post: " + event.getContent());
      notificationPublisher.pushNotification(notification);
    }
  }

  @Transactional
  @KafkaListener(topics = "${kafka.topics.profile-viewed}", groupId = "notification-service-group")
  public void consumeProfileViewed(@Payload ProfileViewedEvent event) {
    log.info("Received ProfileViewedEvent: {}", event);
    Notification notification =
        saveNotification(
            event.getProfileOwnerId(),
            event.getViewerId(),
            "PROFILE_VIEWED",
            "Someone viewed your profile.");
    notificationPublisher.pushNotification(notification);
  }

  private Notification saveNotification(
      java.util.UUID recipientId, java.util.UUID senderId, String type, String message) {
    Notification notification =
        Notification.builder()
            .recipientId(recipientId)
            .senderId(senderId)
            .type(type)
            .message(message)
            .isRead(false)
            .build();
    return notificationRepository.save(notification);
  }
}
