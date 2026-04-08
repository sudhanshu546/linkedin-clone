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
  private String actorProfileId; // Keycloak ID for linking
  private boolean isLikedByCurrentUser;
  private String userReaction;
  private long reactionCount;
  private long commentCount;
}
