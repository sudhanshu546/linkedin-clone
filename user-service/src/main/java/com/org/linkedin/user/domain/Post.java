package com.org.linkedin.user.domain;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posts")
@Data
@Builder
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
}
