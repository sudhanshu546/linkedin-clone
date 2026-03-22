package com.org.linkedin.profile.repo;

import com.org.linkedin.domain.ProfileViewAnalytics;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileViewAnalyticsRepository extends JpaRepository<ProfileViewAnalytics, UUID> {
  Optional<ProfileViewAnalytics> findByProfileOwnerId(UUID profileOwnerId);
}
