package com.org.linkedin.chat.repo;

import com.org.linkedin.domain.chat.ChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
  List<ChatMessage> findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByCreatedDateAsc(
      UUID senderId, UUID recipientId, UUID senderId2, UUID recipientId2);

  @Modifying
  @Transactional
  @Query(
      "UPDATE ChatMessage m SET m.isRead = true WHERE m.recipientId = :recipientId AND m.senderId = :senderId AND m.isRead = false")
  void markMessagesAsRead(UUID recipientId, UUID senderId);

  List<ChatMessage> findBySenderId(UUID senderId);

  List<ChatMessage> findByRecipientId(UUID recipientId);
}
