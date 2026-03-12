package com.org.linkedin.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "activity_feed_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityFeedItem extends AbstractAuditingEntity<UUID> implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "actor_id")
  private UUID actorId;

  @Column(name = "actor_name")
  private String actorName;

  @Column(name = "actor_designation")
  private String actorDesignation;

  @Column(name = "post_id")
  private UUID postId;

  @Column(name = "content", length = 1000)
  private String content;

  @Column(name = "type")
  private String type;

  @Column(name = "image_url")
  private String imageUrl;

  @ElementCollection
  @CollectionTable(name = "feed_item_images", joinColumns = @JoinColumn(name = "feed_item_id"))
  @Column(name = "image_url")
  private java.util.List<String> imageUrls;

  @Column(name = "timestamp")
  private LocalDateTime timestamp;
}
