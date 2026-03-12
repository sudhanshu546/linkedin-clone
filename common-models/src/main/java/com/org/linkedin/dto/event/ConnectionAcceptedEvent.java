package com.org.linkedin.dto.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionAcceptedEvent implements Serializable {
  private UUID requesterId;
  private UUID receiverId;
  private LocalDateTime timestamp;
}
