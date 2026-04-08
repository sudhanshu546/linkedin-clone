package com.org.linkedin.dto.event;

import com.org.linkedin.domain.enumeration.ReactionType;
import java.io.Serializable;
import lombok.Data;

@Data
public class PostReactedEvent implements Serializable {
  private String postId;
  private String postAuthorId;
  private String userId;
  private String userName;
  private String userDesignation;
  private ReactionType reactionType;
}
