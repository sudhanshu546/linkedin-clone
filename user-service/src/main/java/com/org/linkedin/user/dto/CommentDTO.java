package com.org.linkedin.user.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** CommentDTO (flattened) for safe serialization and prevention of circular references. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
  private UUID id;
  private UUID sourcePostId;
  private UUID parentId;
  private UUID authorId;
  private String userName;
  private String userDesignation;
  private String userProfileImageUrl;
  private String content;
  private Long createdDate;
  private Long lastModifiedDate;
}
