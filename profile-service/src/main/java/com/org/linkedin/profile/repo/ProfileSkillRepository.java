package com.org.linkedin.profile.repo;

import com.org.linkedin.domain.ProfileSkill;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileSkillRepository extends JpaRepository<ProfileSkill, UUID> {
  List<ProfileSkill> findByProfileId(UUID profileId);

  Optional<ProfileSkill> findByProfileIdAndSkillId(UUID profileId, UUID skillId);
}
