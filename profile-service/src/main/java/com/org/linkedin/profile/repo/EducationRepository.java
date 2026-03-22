package com.org.linkedin.profile.repo;

import com.org.linkedin.domain.Education;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EducationRepository extends JpaRepository<Education, UUID> {}
