package com.org.linkedin.user.domain;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "posts",
    indexes = {@Index(name = "idx_post_author_id", columnList = "author_id")})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Post extends AbstractAuditingEntity<UUID> {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "post_id")
  private UUID id;

  @Column(name = "author_id", nullable = false)
  private UUID userId;

  @Column(columnDefinition = "TEXT")
  private String content;

  @Column(name = "image_url")
  private String imageUrl; // Still keeping it for the first image or main image

  @ElementCollection
  @CollectionTable(name = "post_images", joinColumns = @JoinColumn(name = "post_id"))
  @Column(name = "image_url")
  private List<String> imageUrls;

  @Column(name = "is_poll")
  private boolean isPoll;

  @Column(name = "poll_question")
  private String pollQuestion;

  @Column(name = "poll_expiry_date")
  private java.time.LocalDateTime pollExpiryDate;

  @Column(name = "comments_disabled")
  private boolean commentsDisabled;
}
