package com.org.linkedin.dto;

import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDTO {
  private UUID id;
  private UUID authorId;
  private String authorName;
  private String authorHeadline;
  private String content;
  private String relationship;
  private String status;
  private String createdAt;
}
