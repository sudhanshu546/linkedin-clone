package com.org.linkedin.profile.domain;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts")
public class Post {
  @Id
  @Column(name = "post_id")
  private UUID postId;

  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @Column(name = "content", length = 5000)
  private String content;

  @Column(name = "image_url")
  private String imageUrl;

  @ElementCollection
  @CollectionTable(name = "post_images", joinColumns = @JoinColumn(name = "post_id"))
  @Column(name = "image_url")
  private List<String> imageUrls;

  @Column(name = "created_at")
  private java.time.LocalDateTime createdAt;
}
