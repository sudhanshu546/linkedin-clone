package com.org.linkedin.dto.poll;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollOptionDTO {
  private UUID id;
  private String text;
  private Long voteCount;
}
