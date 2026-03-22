package com.org.linkedin.profile.repo;

import com.org.linkedin.domain.Profile;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepo extends JpaRepository<Profile, UUID> {
  Profile findByUserId(UUID userId);

  @Query(
      "SELECT p FROM Profile p WHERE "
          + "(:query IS NULL OR (LOWER(p.city) LIKE LOWER(CONCAT('%', :query, '%'))) OR "
          + "(:state IS NULL OR LOWER(p.state) LIKE LOWER(CONCAT('%', :query, '%'))) OR "
          + "(:company IS NULL OR LOWER(p.currentCompany) LIKE LOWER(CONCAT('%', :query, '%'))) OR "
          + "(:headline IS NULL OR LOWER(p.headline) LIKE LOWER(CONCAT('%', :query, '%')))) AND "
          + "(:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND "
          + "(:state IS NULL OR LOWER(p.state) LIKE LOWER(CONCAT('%', :state, '%'))) AND "
          + "(:company IS NULL OR LOWER(p.currentCompany) LIKE LOWER(CONCAT('%', :company, '%'))) AND "
          + "(:headline IS NULL OR LOWER(p.headline) LIKE LOWER(CONCAT('%', :headline, '%')))"
          + " ORDER BY "
          + "CASE WHEN :sortBy = 'name' THEN p.headline ELSE NULL END ASC, "
          + "CASE WHEN :sortBy = 'company' THEN p.currentCompany ELSE NULL END ASC, "
          + "CASE WHEN :sortBy = 'headline' THEN p.headline ELSE NULL END ASC, "
          + "CASE WHEN :sortBy = 'city' THEN p.city ELSE NULL END ASC, "
          + "CASE WHEN :sortBy = 'state' THEN p.state ELSE NULL END ASC, "
          + "p.headline ASC")
  List<Profile> searchProfiles(
      @Param("query") String query,
      @Param("city") String city,
      @Param("state") String state,
      @Param("company") String company,
      @Param("headline") String headline,
      @Param("sortBy") String sortBy);
}
