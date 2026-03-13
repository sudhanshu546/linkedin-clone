package com.org.linkedin.notification.consumer;

import com.org.linkedin.dto.event.*;
import com.org.linkedin.notification.domain.Notification;
import com.org.linkedin.notification.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "${kafka.topics.connection-requested}", groupId = "notification-service-group")
    public void consumeConnectionRequest(@Payload ConnectionRequestedEvent event) {
        log.info("Received ConnectionRequestedEvent: {}", event);
        saveNotification(event.getReceiverId(), event.getSenderId(), "CONNECTION_REQUEST", "You have a new connection request.");
    }

    @KafkaListener(topics = "${kafka.topics.connection-accepted}", groupId = "notification-service-group")
    public void consumeConnectionAccepted(@Payload ConnectionAcceptedEvent event) {
        log.info("Received ConnectionAcceptedEvent: {}", event);
        saveNotification(event.getRequesterId(), event.getReceiverId(), "CONNECTION_ACCEPTED", "Your connection request was accepted.");
    }

    @KafkaListener(topics = "${kafka.topics.post-liked}", groupId = "notification-service-group")
    public void consumePostLiked(@Payload PostLikedEvent event) {
        log.info("Received PostLikedEvent: {}", event);
        if (event.getPostAuthorId() != null) {
            saveNotification(
                java.util.UUID.fromString(event.getPostAuthorId()), 
                java.util.UUID.fromString(event.getUserId()), 
                "POST_LIKED", 
                event.getUserName() + " liked your post."
            );
        }
    }

    @KafkaListener(topics = "${kafka.topics.comment-created}", groupId = "notification-service-group")
    public void consumeCommentCreated(@Payload CommentCreatedEvent event) {
        log.info("Received CommentCreatedEvent: {}", event);
        if (event.getPostAuthorId() != null) {
            saveNotification(
                java.util.UUID.fromString(event.getPostAuthorId()), 
                java.util.UUID.fromString(event.getUserId()), 
                "COMMENT_CREATED", 
                event.getUserName() + " commented on your post: " + event.getContent()
            );
        }
    }

    @KafkaListener(topics = "${kafka.topics.profile-viewed}", groupId = "notification-service-group")
    public void consumeProfileViewed(@Payload ProfileViewedEvent event) {
        log.info("Received ProfileViewedEvent: {}", event);
        saveNotification(
            event.getProfileOwnerId(), 
            event.getViewerId(), 
            "PROFILE_VIEWED", 
            "Someone viewed your profile."
        );
    }

    private void saveNotification(java.util.UUID recipientId, java.util.UUID senderId, String type, String message) {
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .senderId(senderId)
                .type(type)
                .message(message)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }
}
