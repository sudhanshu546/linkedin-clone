package com.org.linkedin.user.repository;

import com.org.linkedin.user.domain.PollOption;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {
  List<PollOption> findByPostId(UUID postId);

  List<PollOption> findByPostIdIn(java.util.Collection<UUID> postIds);

  void deleteByPostId(UUID postId);

  @org.springframework.data.jpa.repository.Modifying
  @org.springframework.data.jpa.repository.Query(
      "UPDATE PollOption p SET p.voteCount = p.voteCount + :delta WHERE p.id = :id")
  void updateVoteCount(UUID id, long delta);
}
