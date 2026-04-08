package com.org.linkedin.user.repository;

import com.org.linkedin.user.domain.PollOption;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {
  List<PollOption> findByPostId(UUID postId);

  void deleteByPostId(UUID postId);
}
