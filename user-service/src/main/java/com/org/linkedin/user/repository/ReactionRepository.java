package com.org.linkedin.user.repository;

import com.org.linkedin.user.domain.Reaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, UUID> {
  Optional<Reaction> findByPostIdAndUserId(UUID postId, UUID userId);

  long countByPostId(UUID postId);

  java.util.List<Reaction> findByPostIdIn(java.util.Collection<UUID> postIds);

  java.util.List<Reaction> findByPostIdInAndUserId(java.util.Collection<UUID> postIds, UUID userId);

  void deleteByPostId(UUID postId);

  void deleteByUserId(UUID id);
}
