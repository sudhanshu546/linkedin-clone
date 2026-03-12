package com.org.linkedin.user.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "post_likes",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"post_id", "user_id"})})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Like {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "post_id", nullable = false)
  private UUID postId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;
}
