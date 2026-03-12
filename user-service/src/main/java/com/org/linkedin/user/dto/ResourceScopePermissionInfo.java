package com.org.linkedin.user.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResourceScopePermissionInfo {
  String id;
  String name;
  List<Scope> scopes;

  @Data
  public static class Scope {
    String id;
    String name;
  }
}
