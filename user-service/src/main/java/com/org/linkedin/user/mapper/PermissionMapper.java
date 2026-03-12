package com.org.linkedin.user.mapper;

import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

  ScopePermissionRepresentation toScopePermission(
      AbstractPolicyRepresentation abstractPolicyRepresentation);

  ResourcePermissionRepresentation toResourcePermission(
      AbstractPolicyRepresentation abstractPolicyRepresentation);
}
