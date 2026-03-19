package com.org.linkedin.user.repository;

import com.org.linkedin.user.domain.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
  List<Comment> findByPostIdOrderByCreatedAtDesc(UUID postId);

  long countByPostId(UUID postId);

  void deleteByPostId(UUID postId);
}
