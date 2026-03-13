package com.org.linkedin.domain.chat;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
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

  @Column(name = "timestamp")
  private LocalDateTime timestamp = LocalDateTime.now();
}
