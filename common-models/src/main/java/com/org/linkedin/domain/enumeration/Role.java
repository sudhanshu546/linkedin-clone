package com.org.linkedin.domain.enumeration;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Role {
  USER("User"),
  SYSTEMADMIN("SystemAdmin"),
  TENANT("Tenant");

  private final String value;

  public String value() {
    return value;
  }
}
