package com.org.linkedin.dto.connection;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionDTO {
  private UUID id;
  private UUID requesterId;
  private UUID receiverId;
  private String status;
  private Long createdAt;
}
