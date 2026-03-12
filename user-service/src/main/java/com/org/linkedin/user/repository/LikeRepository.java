package com.org.linkedin.user.repository;

import com.org.linkedin.user.domain.Like;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {
  Optional<Like> findByPostIdAndUserId(UUID postId, UUID userId);

  long countByPostId(UUID postId);
}
