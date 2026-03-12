package com.org.linkedin.user.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "post_id", nullable = false)
  private UUID postId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "user_name")
  private String userName;

  @Column(name = "user_designation")
  private String userDesignation;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @Column(name = "created_at")
  private LocalDateTime createdAt;
}
