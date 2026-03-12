package com.org.linkedin.user.mapper;

import com.org.linkedin.user.dto.ResourceScopePermissionInfo;
import com.org.linkedin.user.dto.ResourceScopePermissionInfo.Scope;
import java.util.List;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResourceScopePermissionInfoMapper {

  ResourceScopePermissionInfo toResourceScopePermissionInfo(ResourceRepresentation resource);

  List<ResourceScopePermissionInfo> toResourceScopePermissionInfo(
      List<ResourceRepresentation> resources);

  ResourceRepresentation toResource(ResourceScopePermissionInfo resourceScopePermissionInfo);

  List<ResourceRepresentation> toResources(
      List<ResourceScopePermissionInfo> resourceScopePermissionInfos);

  Scope toScope(ScopeRepresentation scope);

  ScopeRepresentation toScopeRepresentation(Scope scope);

  List<Scope> toScopes(List<ScopeRepresentation> scopes);

  List<ScopeRepresentation> toScopeRepresentations(List<Scope> scopes);

  ResourceRepresentation toResource(ResourceRepresentation resource);
}
