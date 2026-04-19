package com.org.linkedin.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.org.linkedin.dto.poll.PollOptionDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** PostDTO (flattened) for safe serialization and prevention of circular references. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
  private UUID id;
  private UUID authorId;
  private String userProfileImageUrl;
  private String content;
  private String imageUrl;
  private List<String> imageUrls;

  @JsonProperty("isPoll")
  private boolean isPoll;

  private String pollQuestion;
  private LocalDateTime pollExpiryDate;
  private List<PollOptionDTO> pollOptions;

  @JsonProperty("hasVoted")
  private boolean hasVoted;

  private UUID selectedOptionId;

  @JsonProperty("likedByCurrentUser")
  private boolean likedByCurrentUser;

  private String userReaction;

  @JsonProperty("commentsDisabled")
  private boolean commentsDisabled;

  private long reactionCount;
  private long commentCount;
  private Long createdDate;
  private Long lastModifiedDate;
}
