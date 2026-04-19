package com.org.linkedin.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.org.linkedin.domain.AbstractAuditingEntity;
import com.org.linkedin.domain.user.TUser;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(
    name = "posts",
    indexes = {@Index(name = "idx_post_author_id", columnList = "author_id")})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE posts SET is_deleted = true WHERE post_id = ?")
@Where(clause = "is_deleted = false OR is_deleted IS NULL")
public class Post extends AbstractAuditingEntity<UUID> {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "post_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private TUser user;

  @Column(name = "author_id", insertable = false, updatable = false)
  private UUID userId;

  @OneToMany(mappedBy = "postId", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Comment> comments = new ArrayList<>();

  @OneToMany(mappedBy = "postId", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private List<Reaction> reactions = new ArrayList<>();

  @OneToMany(mappedBy = "postId", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PollOption> pollOptions = new ArrayList<>();

  @OneToMany(mappedBy = "postId", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PollVote> pollVotes = new ArrayList<>();

  @Column(name = "user_profile_image_url")
  private String userProfileImageUrl;

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
  private LocalDateTime pollExpiryDate;

  @Column(name = "comments_disabled")
  private boolean commentsDisabled;

  @Column(name = "reaction_count")
  private long reactionCount;

  @Column(name = "comment_count")
  private long commentCount;
}
