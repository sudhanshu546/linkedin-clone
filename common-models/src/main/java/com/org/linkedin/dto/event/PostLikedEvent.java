package com.org.linkedin.dto.event;

import java.io.Serializable;
import lombok.Data;

@Data
public class PostLikedEvent implements Serializable {
  private String postId;
  private String userId;
  private String userName;
  private String userDesignation;
}
