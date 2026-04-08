package com.org.linkedin.dto.poll;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollDTO {
  private UUID id;
  private String question;
  private List<PollOptionDTO> options;
  private LocalDateTime expiryDate;
  private boolean hasVoted;
  private UUID selectedOptionId;
}
