package com.org.linkedin.dto;

import com.org.linkedin.domain.enumeration.ReactionType;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostEnrichmentDTO {
  private Map<UUID, Long> reactionCounts;
  private Map<UUID, Long> commentCounts;
  private Map<UUID, ReactionType> userReactions;
}
