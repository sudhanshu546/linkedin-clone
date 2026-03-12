package com.org.linkedin.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConnectionRequestedEvent {
  private UUID senderId;
  private UUID receiverId;
  private LocalDateTime timestamp;
}
