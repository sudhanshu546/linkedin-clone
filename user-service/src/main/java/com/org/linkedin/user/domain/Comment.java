package com.org.linkedin.user.domain;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(
    name = "post_comments",
    indexes = {@Index(name = "idx_comment_post_id", columnList = "post_id")})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE post_comments SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false OR is_deleted IS NULL")
public class Comment extends AbstractAuditingEntity<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @Column(name = "post_id", insertable = false, updatable = false)
  private UUID postId;

  @Column(name = "parent_id")
  private UUID parentId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "user_name")
  private String userName;

  @Column(name = "user_designation")
  private String userDesignation;

  @Column(name = "user_profile_image_url")
  private String userProfileImageUrl;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;
}
