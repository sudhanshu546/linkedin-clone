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
        // recipientId would ideally come from the post metadata or a combined event
        // For now, we log it. In a full implementation, you'd fetch post.authorId
    }

    @KafkaListener(topics = "${kafka.topics.comment-created}", groupId = "notification-service-group")
    public void consumeCommentCreated(@Payload CommentCreatedEvent event) {
        log.info("Received CommentCreatedEvent: {}", event);
        // Similar to PostLiked, need post owner ID
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
