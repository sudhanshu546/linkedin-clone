package com.org.linkedin.profile.repo;

import com.org.linkedin.domain.ActivityFeedItem;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityFeedItemRepository extends JpaRepository<ActivityFeedItem, UUID> {
  Page<ActivityFeedItem> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

  Page<ActivityFeedItem> findByUserIdOrderByPriorityDescTimestampDesc(
      UUID userId, Pageable pageable);
}
