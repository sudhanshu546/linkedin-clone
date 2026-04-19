package com.org.linkedin.chat.consumer;

import com.org.linkedin.chat.repo.ChatMessageRepository;
import com.org.linkedin.domain.chat.ChatMessage;
import com.org.linkedin.dto.event.UserDeletedEvent;
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

  private final ChatMessageRepository chatMessageRepository;

  @KafkaListener(
      topics = "${kafka.topics.user-deleted}",
      groupId = "${spring.kafka.consumer.group-id}")
  @Transactional
  public void consumeUserDeleted(@Payload UserDeletedEvent event) {
    log.info("Received UserDeletedEvent in chat-service for userId: {}", event.getUserId());
    try {
      UUID userId = UUID.fromString(event.getUserId());

      // Soft delete messages sent by the user
      List<ChatMessage> sent = chatMessageRepository.findBySenderId(userId);
      for (ChatMessage m : sent) {
        m.setIsDeleted(true);
      }
      chatMessageRepository.saveAll(sent);

      // Soft delete messages received by the user
      List<ChatMessage> received = chatMessageRepository.findByRecipientId(userId);
      for (ChatMessage m : received) {
        m.setIsDeleted(true);
      }
      chatMessageRepository.saveAll(received);

      log.info("Soft-deleted chat messages for userId: {}", userId);
    } catch (Exception e) {
      log.error("Error processing UserDeletedEvent in chat-service: {}", e.getMessage(), e);
    }
  }
}
