package com.org.linkedin.dto.auditLog;

import com.org.linkedin.dto.BaseDTO;
import java.io.Serializable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/** A DTO for the {@link com.hos.iot.domain.AuditLog} entity. */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLogDTO extends BaseDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  UUID id;

  String endPoint;

  String method;

  String ipAddress;

  String status;

  Boolean isError;

  Long date;

  UUID userId;
}
