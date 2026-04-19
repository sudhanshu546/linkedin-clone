package com.org.linkedin.dto.event;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeletedEvent implements Serializable {
  private String userId;
  private String email;
  private long timestamp;
}
