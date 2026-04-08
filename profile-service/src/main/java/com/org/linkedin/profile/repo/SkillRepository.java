package com.org.linkedin.profile.repo;

import com.org.linkedin.domain.Skill;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {
  Optional<Skill> findByNameIgnoreCase(String name);
}
