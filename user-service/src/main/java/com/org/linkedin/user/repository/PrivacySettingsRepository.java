package com.org.linkedin.user.repository;

import com.org.linkedin.user.domain.PrivacySettings;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivacySettingsRepository extends JpaRepository<PrivacySettings, UUID> {}
