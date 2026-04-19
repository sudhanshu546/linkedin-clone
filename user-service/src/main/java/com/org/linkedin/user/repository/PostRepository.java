package com.org.linkedin.user.repository;

import com.org.linkedin.user.domain.Post;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
  java.util.List<Post> findByUserIdOrderByCreatedDateDesc(UUID userId);

  Page<Post> findByUserIdOrderByCreatedDateDesc(UUID userId, Pageable pageable);

  void deleteByUserId(UUID userId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "UPDATE Post p SET p.reactionCount = GREATEST(0, p.reactionCount + :delta) WHERE p.id = :postId")
  void updateReactionCount(@Param("postId") UUID postId, @Param("delta") int delta);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "UPDATE Post p SET p.commentCount = GREATEST(0, p.commentCount + :delta) WHERE p.id = :postId")
  void updateCommentCount(@Param("postId") UUID postId, @Param("delta") int delta);
}
