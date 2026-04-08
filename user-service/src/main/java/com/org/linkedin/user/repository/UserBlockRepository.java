package com.org.linkedin.user.repository;

import com.org.linkedin.user.domain.UserBlock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {
  boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

  Optional<UserBlock> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

  List<UserBlock> findByBlockerId(UUID blockerId);
}
