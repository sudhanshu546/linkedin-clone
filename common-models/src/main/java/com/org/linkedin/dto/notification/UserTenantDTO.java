package com.org.linkedin.dto.notification;

import java.util.UUID;
import lombok.Data;

@Data
public class UserTenantDTO {

  private UUID id;
  private UUID tenantId;

  public UserTenantDTO(UUID id, UUID tenantId) {
    this.id = id;
    this.tenantId = tenantId;
  }

  public UserTenantDTO() {}
}
