package com.org.linkedin.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "activity_feed_item",
    indexes = {
      @Index(
          name = "idx_feed_user_priority_ts",
          columnList = "user_id, priority DESC, timestamp DESC"),
      @Index(name = "idx_feed_user_ts", columnList = "user_id, timestamp DESC")
    })
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
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

  @Column(name = "actor_avatar")
  private String actorAvatar;

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

  @Column(name = "priority")
  private Double priority;

  @ElementCollection
  @MapKeyColumn(name = "metadata_key")
  @Column(name = "metadata_value")
  @CollectionTable(name = "feed_item_metadata", joinColumns = @JoinColumn(name = "feed_item_id"))
  private java.util.Map<String, String> metadata;
}
