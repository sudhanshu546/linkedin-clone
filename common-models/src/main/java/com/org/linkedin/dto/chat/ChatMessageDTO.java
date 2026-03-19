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
public class ChatMessageDTO {
  private UUID id;
  private UUID senderId;
  private UUID recipientId;
  private String content;
  private boolean isRead;
  private Long timestamp;
}
