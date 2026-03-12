package com.org.linkedin.user.controller;

import com.org.linkedin.constants.Constants;
import com.org.linkedin.dto.BasePageResponse;
import com.org.linkedin.dto.BaseResponse;
import com.org.linkedin.dto.user.RoleDTO;
import com.org.linkedin.user.service.KeyCloakAdminService;
import com.org.linkedin.user.utility.PaginationUtil;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("${apiPrefix}/kc")
@Slf4j
@AllArgsConstructor
public class KeyCloakAdminController {

  private final KeyCloakAdminService keyCloakAdminService;

  //  @PreAuthorize("hasRole('SystemAdmin')")
  @PostMapping("/role")
  public ResponseEntity<BaseResponse<RoleDTO>> save(@RequestBody @Valid RoleDTO requestedRole) {
    log.trace("Enter save method :: :: requestedRole {}", requestedRole);
    RoleDTO role = keyCloakAdminService.save(requestedRole);
    BaseResponse<RoleDTO> returnValue =
        BaseResponse.<RoleDTO>builder().status(HttpStatus.CREATED.value()).result(role).build();
    log.trace("Exit save method :: [{}]", returnValue);
    return ResponseEntity.ok(returnValue);
  }

  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant')")
  @GetMapping("/role")
  public ResponseEntity<BasePageResponse<List<RoleDTO>>> getAllRoles(Pageable pageable) {
    log.trace("Enter getAllRoles method ");
    Page<RoleDTO> roles = keyCloakAdminService.getAllRoles(pageable);
    HttpHeaders headers =
        PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), roles);
    BasePageResponse<List<RoleDTO>> returnValue =
        BasePageResponse.<List<RoleDTO>>builder()
            .pageNumber(roles.getNumber())
            .pageSize(roles.getSize())
            .status(HttpStatus.OK.value())
            .result(roles.getContent())
            .totalRecords(roles.getTotalElements())
            .build();
    log.trace("Exit getAllRoles method :: [{}]", returnValue);
    return ResponseEntity.ok().headers(headers).body(returnValue);
  }

  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant')")
  @GetMapping("/role/{id}")
  public ResponseEntity<BaseResponse<RoleDTO>> getRoleById(@PathVariable("id") UUID roleId) {
    log.trace("Enter getRoleById method :: :: roleId :: [{}]", roleId);
    RoleDTO role = keyCloakAdminService.getRoleById(roleId);
    BaseResponse<RoleDTO> returnValue =
        BaseResponse.<RoleDTO>builder().status(HttpStatus.OK.value()).result(role).build();
    log.trace("Exit getRoleById method :: [{}]", returnValue);
    return ResponseEntity.ok().body(returnValue);
  }

  @PreAuthorize("hasRole('SystemAdmin')")
  @PostMapping("/assignRole")
  public ResponseEntity<BaseResponse<RoleDTO>> assignRoleToUser(
      @RequestParam("roleId") UUID roleId, @RequestParam("keycloakUserId") UUID userId) {
    log.trace("Enter assignRoleToUser method :: :: roleId :: [{}]", roleId);
    keyCloakAdminService.assignRoleToUser(roleId, userId);
    BaseResponse<RoleDTO> returnValue =
        BaseResponse.<RoleDTO>builder()
            .status(HttpStatus.OK.value())
            .message("ROLE_ASSIGNED_SUCCESSFULLY")
            .build();
    log.trace("Exit assignRoleToUser method :: [{}]", returnValue);
    return ResponseEntity.ok().body(returnValue);
  }

  @PreAuthorize("hasRole('SystemAdmin')")
  @DeleteMapping("/role/{id}")
  public ResponseEntity<BaseResponse<Void>> deleteRole(@PathVariable("id") UUID roleId) {
    log.trace("Enter deleteRole method :: :: roleId :: [{}]", roleId);
    keyCloakAdminService.deleteRole(roleId);
    BaseResponse<Void> returnValue =
        BaseResponse.<Void>builder()
            .status(HttpStatus.OK.value())
            .result(null)
            .message(Constants.OPERATION_SUCCESSFUL)
            .build();
    log.trace("Exit deleteRole method :: [{}]", returnValue);
    return ResponseEntity.ok().body(returnValue);
  }

  @PreAuthorize("hasRole('SystemAdmin')")
  @PutMapping("/role/{id}")
  public ResponseEntity<BaseResponse<RoleDTO>> updateRole(
      @PathVariable("id") UUID roleId, @RequestBody RoleDTO requestedRole) {
    log.trace("Enter updateRole method :: :: roleId :: [{}]", roleId);
    RoleDTO role = keyCloakAdminService.updateRole(roleId, requestedRole);
    BaseResponse<RoleDTO> returnValue =
        BaseResponse.<RoleDTO>builder().status(HttpStatus.OK.value()).result(role).build();
    log.trace("Exit updateRole method :: [{}]", returnValue);
    return ResponseEntity.ok().body(returnValue);
  }

  //  @PreAuthorize("hasRole('SystemAdmin')")
  //  @GetMapping("/resources")
  //  public ResponseEntity<BaseResponse<List<ResourceScopePermissionInfo>>> getResources(
  //      Authentication authentication) {
  //    List<ResourceScopePermissionInfo> resources = keyCloakAdminService.getResources();
  //    BaseResponse<List<ResourceScopePermissionInfo>> returnValue =
  //        BaseResponse.<List<ResourceScopePermissionInfo>>builder()
  //            .status(HttpStatus.OK.value())
  //            .result(resources)
  //            .build();
  //    log.trace("Exit getUserDetailsById method :: [{}]", returnValue);
  //    return ResponseEntity.ok(returnValue);
  //  }

  //  @PreAuthorize("hasRole('SystemAdmin')")
  //  @GetMapping("/role/{roleId}/permissions")
  //  public ResponseEntity<BaseResponse<List<ResourceScopePermissionInfo>>> getPermissionsByRoleId(
  //      @PathVariable(value = "roleId") UUID roleId) {
  //    log.trace("Enter getResourcesByRoleId method :: ::  roleId :: [{}]", roleId);
  //    List<ResourceScopePermissionInfo> permissions =
  //        keyCloakAdminService.getPermissionsByRoleId(roleId);
  //    BaseResponse<List<ResourceScopePermissionInfo>> returnValue =
  //        BaseResponse.<List<ResourceScopePermissionInfo>>builder()
  //            .status(HttpStatus.OK.value())
  //            .result(permissions)
  //            .build();
  //    log.trace("Exit getResourcesByRoleId method :: [{}]", returnValue);
  //    return ResponseEntity.ok(returnValue);
  //  }

  //  @PreAuthorize("hasRole('SystemAdmin')")
  //  @GetMapping("/role/{roleId}/unlinked-permissions")
  //  public ResponseEntity<BaseResponse<List<ResourceScopePermissionInfo>>>
  //      getNonAssociatedRoleIdPermissions(@PathVariable(value = "roleId") UUID roleId) {
  //    log.trace("Enter getNonAssociatedRoleIdPermissions method :: ::  roleId :: [{}]", roleId);
  //    List<ResourceScopePermissionInfo> permissions =
  //        keyCloakAdminService.getNonAssociatedRoleIdPermissions(roleId);
  //    BaseResponse<List<ResourceScopePermissionInfo>> returnValue =
  //        BaseResponse.<List<ResourceScopePermissionInfo>>builder()
  //            .status(HttpStatus.OK.value())
  //            .result(permissions)
  //            .build();
  //    log.trace("Exit getNonAssociatedRoleIdPermissions method :: [{}]", returnValue);
  //    return ResponseEntity.ok(returnValue);
  //  }

  //  @PreAuthorize("hasRole('SystemAdmin')")
  //  @PostMapping("/role/{roleId}/permissions")
  //  public ResponseEntity<BaseResponse<List<ResourceScopePermissionInfo>>>
  // updatePermissionsByRoleId(
  //      @PathVariable(value = "roleId") UUID roleId,
  //      @RequestBody List<ResourceScopePermissionInfo> updatedResources) {
  //    log.trace("Enter updatePermissionsByRoleId method :: ::  roleId :: [{}]", roleId);
  //    List<ResourceScopePermissionInfo> permissions =
  //        keyCloakAdminService.updatePermissionsByRoleId(roleId, updatedResources);
  //    BaseResponse<List<ResourceScopePermissionInfo>> returnValue =
  //        BaseResponse.<List<ResourceScopePermissionInfo>>builder()
  //            .status(HttpStatus.OK.value())
  //            .result(permissions)
  //            .build();
  //    log.trace("Exit updatePermissionsByRoleId method :: [{}]", returnValue);
  //    return ResponseEntity.ok(returnValue);
  //  }
}
