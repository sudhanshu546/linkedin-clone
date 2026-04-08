package com.org.linkedin.dto.event;

import com.org.linkedin.dto.job.JobDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobUpdatedEvent {
  private JobDTO job;
  private String action; // CREATE, UPDATE, DELETE
}
