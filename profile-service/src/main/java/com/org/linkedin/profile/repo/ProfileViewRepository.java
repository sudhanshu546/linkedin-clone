package com.org.linkedin.profile.repo;

import com.org.linkedin.profile.domain.ProfileView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProfileViewRepository extends JpaRepository<ProfileView, UUID> {
    List<ProfileView> findByProfileOwnerIdOrderByViewedAtDesc(UUID profileOwnerId);
    long countByProfileOwnerId(UUID profileOwnerId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT CAST(viewed_at AS DATE) as view_date, COUNT(*) as view_count " +
            "FROM profile_views WHERE profile_owner_id = :ownerId " +
            "AND viewed_at >= :since " +
            "GROUP BY CAST(viewed_at AS DATE) ORDER BY view_date ASC", nativeQuery = true)
    List<Object[]> getDailyViewTrends(@org.springframework.data.repository.query.Param("ownerId") UUID ownerId, 
                                     @org.springframework.data.repository.query.Param("since") LocalDateTime since);
}
