package com.org.linkedin.dto.event;

import java.io.Serializable;
import lombok.Data;

@Data
public class CommentCreatedEvent implements Serializable {
  private String commentId;
  private String postId;
  private String userId;
  private String userName;
  private String userDesignation;
  private String content;
}
