package com.org.linkedin.domain.auditLog;

import com.org.linkedin.domain.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/** A Logger. */
@Entity
@Table(name = "audit_log")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLog extends AbstractAuditingEntity<UUID> implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @NotNull
  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false)
  UUID id;

  @Column(name = "endPoint")
  String endPoint;

  @Column(name = "method")
  String method;

  @Column(name = "ipAddress")
  String ipAddress;

  @Column(name = "status")
  String status;

  @Column(name = "isError")
  Boolean isError;

  @Column(name = "date")
  Long date;

  @Column(name = "userId")
  UUID userId;
}
