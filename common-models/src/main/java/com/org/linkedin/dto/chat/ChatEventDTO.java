package com.org.linkedin.dto.chat;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEventDTO {
  public enum EventType {
    PRESENCE,
    TYPING,
    READ_RECEIPT
  }

  private EventType type;
  private UUID senderId;
  private UUID recipientId;
  private boolean online;
  private boolean typing;
  private UUID messageId;
  private long timestamp;
}
