package com.org.linkedin.profile.repo;

import com.org.linkedin.domain.Experience;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExperienceRepository extends JpaRepository<Experience, UUID> {}
