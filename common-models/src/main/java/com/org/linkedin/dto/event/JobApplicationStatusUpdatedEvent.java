package com.org.linkedin.dto.event;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationStatusUpdatedEvent {
  private UUID jobId;
  private UUID applicantId;
  private String status;
  private String jobTitle;
  private long timestamp;
}
