package com.org.linkedin.user.service;

// import com.hos.iot.user.domain.Role;
import com.org.linkedin.domain.user.Role;
import com.org.linkedin.dto.user.RoleDTO;
import com.org.linkedin.user.dto.ResourceScopePermissionInfo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// import com.hos.iot.user.dto.RoleDTO;

public interface KeyCloakAdminService {
  List<ResourceScopePermissionInfo> getResources();

  RoleDTO save(RoleDTO requestedRole);

  Page<RoleDTO> getAllRoles(Pageable pageable);

  RoleDTO getRoleById(UUID roleId);

  RoleDTO updateRole(UUID roleId, RoleDTO requestedRole);

  void deleteRole(UUID roleId);

  List<ResourceScopePermissionInfo> getPermissionsByRoleId(UUID roleId);

  Role getRole(UUID roleId);

  List<ResourceScopePermissionInfo> updatePermissionsByRoleId(
      UUID roleId, List<ResourceScopePermissionInfo> updatedResources);

  List<ResourceScopePermissionInfo> getNonAssociatedRoleIdPermissions(UUID roleId);

  void assignRoleToUser(UUID roleId, UUID userId);
}
