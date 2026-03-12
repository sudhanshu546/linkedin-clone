package com.org.linkedin.user.repository.auditLog;

import com.org.linkedin.domain.auditLog.AuditLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for the AuditLog entity. */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
  List<AuditLog> findAllByIsDeletedFalse();

  Optional<AuditLog> findByIdAndIsDeletedFalse(UUID id);
}
