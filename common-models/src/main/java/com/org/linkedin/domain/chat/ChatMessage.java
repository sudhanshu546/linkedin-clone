package com.org.linkedin.domain.chat;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "messages")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage extends AbstractAuditingEntity<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "sender_id", nullable = false)
  private UUID senderId;

  @Column(name = "recipient_id", nullable = false)
  private UUID recipientId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Builder.Default
  @Column(name = "is_read", nullable = false)
  private boolean isRead = false;
}
