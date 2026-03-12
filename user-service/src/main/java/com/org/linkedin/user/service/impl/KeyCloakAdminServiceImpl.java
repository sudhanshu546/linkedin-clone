package com.org.linkedin.user.service.impl;

import com.org.linkedin.domain.user.Role;
import com.org.linkedin.domain.user.TUser;
import com.org.linkedin.dto.user.RoleDTO;
import com.org.linkedin.user.config.keycloak.KeycloakClients;
import com.org.linkedin.user.dto.ResourceScopePermissionInfo;
import com.org.linkedin.user.dto.ResourceScopePermissionInfo.Scope;
import com.org.linkedin.user.mapper.ResourceScopePermissionInfoMapper;
import com.org.linkedin.user.mapper.RoleMapper;
import com.org.linkedin.user.repository.RoleRepository;
import com.org.linkedin.user.repository.UserRepository;
import com.org.linkedin.user.service.KeyCloakAdminService;
import com.org.linkedin.user.utility.KeyCloakUtil;
import com.org.linkedin.utility.errors.ErrorKeys;
import com.org.linkedin.utility.exception.CommonExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@AllArgsConstructor
@Service
@Transactional
public class KeyCloakAdminServiceImpl implements KeyCloakAdminService {

  private final KeyCloakUtil keyCloakUtil;

  private final RoleMapper roleMapper;

  private final RoleRepository roleRepository;

  private final ResourceScopePermissionInfoMapper resourceScopePermissionInfoMapper;

  private final KeycloakClients keycloakClient;

  private final UserRepository userRepository;

  @Override
  public List<ResourceScopePermissionInfo> getResources() {
    List<ResourceRepresentation> resources = keyCloakUtil.getResources();
    List<ResourceScopePermissionInfo> resourceScopePermissionInfos =
        resourceScopePermissionInfoMapper.toResourceScopePermissionInfo(resources);
    return resourceScopePermissionInfos;
  }

  @Override
  public RoleDTO save(RoleDTO requestedRole) {
    validateRole(requestedRole);
    RoleRepresentation roleRepresentation = roleMapper.toRoleRepresentation(requestedRole);
    ClientResource clientResource = keyCloakUtil.getClientResource(keycloakClient);
    RoleRepresentation keyCloackRole =
        keyCloakUtil.addRoleToKeyCloak(clientResource, roleRepresentation);
    keyCloakUtil.getPolicyByRole(clientResource, keyCloackRole);
    Role role = roleMapper.toEntity(requestedRole);
    role.setKeyCloakRoleId(keyCloackRole.getId());
    roleRepository.save(role);
    RoleDTO roleDTO = roleMapper.toDto(role);
    return roleDTO;
  }

  private void validateRoleName(RoleDTO requestedRole, String dbRoleName, String roleCode) {
    if (!StringUtils.equals(dbRoleName, requestedRole.getName())
        && !StringUtils.equals(roleCode, requestedRole.getCode())
        && roleRepository.existsByNameOrCode(requestedRole.getName(), requestedRole.getCode())) {
      throw new CommonExceptionHandler(
          ErrorKeys.ROLE_ALREADY_EXISTS, HttpStatus.BAD_REQUEST.value());
    }
  }

  private void validateRole(RoleDTO requestedRole) {
    if (roleRepository.existsByNameOrCode(requestedRole.getName(), requestedRole.getCode())) {
      throw new CommonExceptionHandler(
          ErrorKeys.ROLE_ALREADY_EXISTS, HttpStatus.BAD_REQUEST.value());
    }
  }

  @Override
  public Page<RoleDTO> getAllRoles(Pageable pageable) {
    Page<Role> roles = roleRepository.findAll(pageable);
    Page<RoleDTO> rolesPage = roles.map(roleMapper::toDto);
    return rolesPage;
  }

  @Override
  public RoleDTO getRoleById(UUID roleId) {
    Role role = getRole(roleId);
    RoleDTO roleDTO = roleMapper.toDto(role);
    return roleDTO;
  }

  @Override
  public Role getRole(UUID roleId) {
    Optional<Role> role = roleRepository.findById(roleId);
    if (role.isEmpty()) {
      throw new CommonExceptionHandler(ErrorKeys.INVALID_ROLE_ID, HttpStatus.BAD_REQUEST.value());
    }
    return role.get();
  }

  @Override
  public RoleDTO updateRole(UUID roleId, RoleDTO requestedRole) {
    Role dbRole = getRole(roleId);
    String dbRoleName = dbRole.getName();
    validateRoleName(requestedRole, dbRoleName, dbRole.getCode());
    Role role = roleMapper.toEntity(requestedRole);
    ClientResource clientResource = keyCloakUtil.getClientResource(keycloakClient);
    RoleRepresentation keyCloackRole = roleMapper.toRoleRepresentation(requestedRole);
    keyCloackRole.setId(dbRole.getKeyCloakRoleId());
    RoleRepresentation updatedRole =
        keyCloakUtil.updateRole(clientResource, dbRoleName, keyCloackRole);
    role.setKeyCloakRoleId(updatedRole.getId());
    roleRepository.save(role);
    RolePolicyRepresentation policy = keyCloakUtil.getPolicyByRole(clientResource, dbRoleName);
    keyCloakUtil.updatePolicyByRole(clientResource, updatedRole.getName(), policy);
    RoleDTO roleDTO = roleMapper.toDto(role);
    return roleDTO;
  }

  @Override
  public void deleteRole(UUID roleId) {
    Role role = getRole(roleId);
    ClientResource clientResource = keyCloakUtil.getClientResource(keycloakClient);
    keyCloakUtil.deleteRole(
        clientResource,
        role.getKeyCloakRoleId()); // deleting role from keycloak deletes policies also
    role.setIsDeleted(true);
    roleRepository.save(role);
  }

  @Override
  public List<ResourceScopePermissionInfo> getPermissionsByRoleId(UUID roleId) {
    Role role = getRole(roleId);
    ClientResource clientResource = keyCloakUtil.getClientResource(keycloakClient);
    String keyCloackRoleId = role.getKeyCloakRoleId();
    List<ResourceRepresentation> roleResources =
        keyCloakUtil.getResourcesByRoleId(keyCloackRoleId, clientResource);
    List<ResourceScopePermissionInfo> resourceScopePermissionInfos =
        resourceScopePermissionInfoMapper.toResourceScopePermissionInfo(roleResources);
    return resourceScopePermissionInfos;
  }

  @Override
  public List<ResourceScopePermissionInfo> updatePermissionsByRoleId(
      UUID roleId, List<ResourceScopePermissionInfo> updatedResources) {
    Role role = getRole(roleId);
    updatedResources = processResources(updatedResources);
    ClientResource clientResource = keyCloakUtil.getClientResource(keycloakClient);
    String keyCloackRoleId = role.getKeyCloakRoleId();
    List<ResourceRepresentation> updatedKeyCloakResources =
        resourceScopePermissionInfoMapper.toResources(updatedResources);
    List<ResourceRepresentation> roleResources =
        keyCloakUtil.getResourcesByRoleId(keyCloackRoleId, clientResource);
    keyCloakUtil.updatePermissionsByRoleId(
        roleResources, clientResource, updatedKeyCloakResources, keyCloackRoleId);
    List<ResourceRepresentation> updatedRoleResources =
        keyCloakUtil.getResourcesByRoleId(keyCloackRoleId, clientResource);
    List<ResourceScopePermissionInfo> resourceScopePermissionInfos =
        resourceScopePermissionInfoMapper.toResourceScopePermissionInfo(updatedRoleResources);
    return resourceScopePermissionInfos;
  }

  private List<ResourceScopePermissionInfo> processResources(
      List<ResourceScopePermissionInfo> updatedResources) {
    List<ResourceScopePermissionInfo> resources = new ArrayList<>();
    for (ResourceScopePermissionInfo requestedResource : updatedResources) {
      boolean isResoureEncountered = false;
      for (ResourceScopePermissionInfo aggregatedResource : resources) {
        if (aggregatedResource.getId().equals(requestedResource.getId())) {
          List<Scope> aggregatedScopes = aggregatedResource.getScopes();
          List<Scope> requestedScope = requestedResource.getScopes();
          aggregatedScopes.addAll(requestedScope);
          isResoureEncountered = true;
          break;
        }
      }
      if (!isResoureEncountered) {
        resources.add(requestedResource);
      }
    }
    return resources;
  }

  @Override
  public List<ResourceScopePermissionInfo> getNonAssociatedRoleIdPermissions(UUID roleId) {
    List<ResourceRepresentation> keyCloakResources = keyCloakUtil.getResources();
    ClientResource clientResource = keyCloakUtil.getClientResource(keycloakClient);
    Role role = getRole(roleId);
    String keyCloackRoleId = role.getKeyCloakRoleId();
    List<ResourceRepresentation> roleResources =
        keyCloakUtil.getResourcesByRoleId(keyCloackRoleId, clientResource);
    List<ResourceRepresentation> unassociatedRoleRescourcesScopes =
        filterUnAssociatedRolesScopes(keyCloakResources, roleResources);
    List<ResourceScopePermissionInfo> unassociatedPermissions =
        resourceScopePermissionInfoMapper.toResourceScopePermissionInfo(
            unassociatedRoleRescourcesScopes);
    return unassociatedPermissions;
  }

  @Override
  public void assignRoleToUser(UUID roleId, UUID userId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + roleId));
    TUser user =
        userRepository
            .findByKeycloakUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("TUser not found with id: " + userId));
    ClientResource clientResource = keyCloakUtil.getClientResource(keycloakClient);
    keyCloakUtil.assignRoleToEmployee(clientResource, userId.toString(), role.getName());
  }

  private List<ResourceRepresentation> filterUnAssociatedRolesScopes(
      List<ResourceRepresentation> keyCloakResources, List<ResourceRepresentation> roleResources) {
    keyCloakResources.stream().sorted(Comparator.comparing(ResourceRepresentation::getName));
    roleResources.stream().sorted(Comparator.comparing(ResourceRepresentation::getName));
    List<ResourceRepresentation> unassociatedRoleRescourcesScopes = new ArrayList<>();
    for (ResourceRepresentation resource : keyCloakResources) {
      boolean isNewResourceScopePresent = false;
      for (ResourceRepresentation roleResource : roleResources) {
        if (roleResource.getId().equals(resource.getId())) {
          isNewResourceScopePresent = true;
          Set<ScopeRepresentation> keyCloakResourceScopes = resource.getScopes();
          Set<ScopeRepresentation> roleResourceScopes = roleResource.getScopes();
          if (!CollectionUtils.isEmpty(keyCloakResourceScopes)) {
            fetchUnassociatedPermissionsByResourceScope(
                roleResource,
                keyCloakResourceScopes,
                roleResourceScopes,
                unassociatedRoleRescourcesScopes);
          }
          break;
        }
      }
      if (!isNewResourceScopePresent) {
        unassociatedRoleRescourcesScopes.add(resource);
      }
    }
    return unassociatedRoleRescourcesScopes;
  }

  private void fetchUnassociatedPermissionsByResourceScope(
      ResourceRepresentation roleResource,
      Set<ScopeRepresentation> keyCloakResourceScopes,
      Set<ScopeRepresentation> roleResourceScopes,
      List<ResourceRepresentation> unassociatedRoleRescourcesScopes) {
    Set<ScopeRepresentation> resourceNewScopes = new HashSet<>();
    keyCloakResourceScopes.stream().sorted(Comparator.comparing(ScopeRepresentation::getName));
    roleResourceScopes.stream().sorted(Comparator.comparing(ScopeRepresentation::getName));
    for (ScopeRepresentation keyCloakResourceScope : keyCloakResourceScopes) {
      boolean isRescourceScopePresent = false;
      for (ScopeRepresentation roleResourceScope : roleResourceScopes) {
        if (roleResourceScope.getId().equals(keyCloakResourceScope.getId())) {
          isRescourceScopePresent = true;
          break;
        }
      }
      if (!isRescourceScopePresent) {
        resourceNewScopes.add(keyCloakResourceScope);
      }
    }
    if (!CollectionUtils.isEmpty(resourceNewScopes)) {
      // need to add the scopes to the requested resource
      ResourceRepresentation newResourceScope =
          resourceScopePermissionInfoMapper.toResource(roleResource);
      newResourceScope.setScopes(resourceNewScopes);
      unassociatedRoleRescourcesScopes.add(newResourceScope);
    }
  }
}
