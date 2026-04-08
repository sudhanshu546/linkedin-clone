package com.org.linkedin.user.repository;

import com.org.linkedin.user.domain.Post;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
  java.util.List<Post> findByUserIdOrderByCreatedDateDesc(UUID userId);

  Page<Post> findByUserIdOrderByCreatedDateDesc(UUID userId, Pageable pageable);

  void deleteByUserId(UUID userId);
}
