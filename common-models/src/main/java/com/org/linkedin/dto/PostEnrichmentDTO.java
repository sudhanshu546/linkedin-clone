package com.org.linkedin.dto;

import com.org.linkedin.dto.poll.PollOptionDTO;
import java.util.List;
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
  private Map<UUID, com.org.linkedin.domain.enumeration.ReactionType> userReactions;

  // Poll Enrichment
  private Map<UUID, List<PollOptionDTO>> pollOptions;
  private Map<UUID, Boolean> hasVoted;
  private Map<UUID, UUID> selectedOptionIds;
}
