package com.org.linkedin.dto.connection;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConnectionStatusDTO implements Serializable {
  private UUID connectionId;
  private String status; // PENDING, ACCEPTED, REJECTED, null (NONE)
  private boolean isRequester; // true if the logged-in user is the requester
}
