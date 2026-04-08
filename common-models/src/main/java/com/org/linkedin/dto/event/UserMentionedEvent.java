package com.org.linkedin.dto.event;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMentionedEvent implements Serializable {
  private String postId;
  private String postAuthorId;
  private String postAuthorName;
  private String mentionedUserId;
  private String snippet;
  private long timestamp;
}
