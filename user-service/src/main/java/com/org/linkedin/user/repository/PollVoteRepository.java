package com.org.linkedin.user.repository;

import com.org.linkedin.user.domain.PollVote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {
  Optional<PollVote> findByPostIdAndUserId(UUID postId, UUID userId);

  List<PollVote> findByPostIdInAndUserId(java.util.Collection<UUID> postIds, UUID userId);

  long countByOptionId(UUID optionId);

  void deleteByPostId(UUID postId);
}
