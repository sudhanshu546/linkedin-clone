package com.org.linkedin.dto.connection;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConnectionResponseDTO {
  private UUID id;
  private UUID userId;
  private String status;
}
