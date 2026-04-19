package com.org.linkedin.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityFeedItemDTO {
  private UUID id;
  private UUID userId;
  private UUID actorId;
  private UUID postId;
  private String content;
  private String type;
  private String imageUrl;
  private java.util.List<String> imageUrls;
  private LocalDateTime timestamp;

  // Actor details for LinkedIn-like display
  private String actorName;
  private String actorDesignation;
  private String actorAvatar;
  private String actorProfileId; // Keycloak ID for linking

  @com.fasterxml.jackson.annotation.JsonProperty("likedByCurrentUser")
  private boolean likedByCurrentUser;

  private String userReaction;
  private long reactionCount;
  private long commentCount;

  // Poll Details
  @com.fasterxml.jackson.annotation.JsonProperty("isPoll")
  private boolean isPoll;

  private String pollQuestion;
  private java.util.List<com.org.linkedin.dto.poll.PollOptionDTO> pollOptions;

  @com.fasterxml.jackson.annotation.JsonProperty("hasVoted")
  private boolean hasVoted;

  private UUID selectedOptionId;
}
