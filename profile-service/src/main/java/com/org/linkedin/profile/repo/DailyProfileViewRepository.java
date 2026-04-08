package com.org.linkedin.profile.repo;

import com.org.linkedin.profile.domain.DailyProfileView;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyProfileViewRepository extends JpaRepository<DailyProfileView, UUID> {
  Optional<DailyProfileView> findByProfileOwnerIdAndViewDate(
      UUID profileOwnerId, LocalDate viewDate);

  List<DailyProfileView> findByProfileOwnerIdAndViewDateAfterOrderByViewDateAsc(
      UUID profileOwnerId, LocalDate date);
}
