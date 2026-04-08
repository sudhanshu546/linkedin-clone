package com.org.linkedin.user.domain;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "post_comments",
    indexes = {@Index(name = "idx_comment_post_id", columnList = "post_id")})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Comment extends AbstractAuditingEntity<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "post_id", nullable = false)
  private UUID postId;

  @Column(name = "parent_id")
  private UUID parentId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "user_name")
  private String userName;

  @Column(name = "user_designation")
  private String userDesignation;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;
}
