package com.org.linkedin.dto.event;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class PostCreatedEvent implements Serializable {
  private String postId;
  private String userId;
  private String userName;
  private String userDesignation;
  private String userProfileImageUrl;
  private String content;
  private String imageUrl; // For backward compatibility or first image
  private List<String> imageUrls;

  private boolean isPoll;
  private String pollQuestion;
  private List<String> pollOptions;
  private java.time.LocalDateTime pollExpiryDate;
}
