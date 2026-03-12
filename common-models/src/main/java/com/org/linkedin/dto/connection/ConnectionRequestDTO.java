package com.org.linkedin.dto.connection;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConnectionRequestDTO {
  private UUID receiverId;
}
