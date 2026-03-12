package com.org.linkedin.user.mapper;

import com.org.linkedin.domain.user.Role;
import com.org.linkedin.dto.user.RoleDTO;
import com.org.linkedin.user.dto.CommonListDto;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper extends EntityMapper<RoleDTO, Role> {
  @Mapping(source = "id", target = "id")
  @Mapping(source = "name", target = "name")
  @Mapping(source = "code", target = "code")
  CommonListDto toListDto(Role role);

  @Mapping(target = "id", ignore = true)
  RoleRepresentation toRoleRepresentation(RoleDTO role);

  @Mapping(target = "keyCloakRoleId", source = "id")
  Role toRole(RoleRepresentation role);
}
