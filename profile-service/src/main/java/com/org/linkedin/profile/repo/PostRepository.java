package com.org.linkedin.profile.repo;

import com.org.linkedin.profile.domain.Post;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
  void deleteByPostId(UUID postId);

  Page<Post> findByAuthorIdOrderByCreatedDateDesc(UUID authorId, Pageable pageable);

  List<Post> findByAuthorId(UUID id);
}
