package com.org.linkedin.profile.repo;

import com.org.linkedin.domain.Recommendation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {
  List<Recommendation> findByProfileIdAndStatus(UUID profileId, String status);

  List<Recommendation> findByAuthorId(UUID authorId);
}
