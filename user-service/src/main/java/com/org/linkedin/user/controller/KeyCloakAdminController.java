package com.org.linkedin.user.controller;

import com.org.linkedin.constants.Constants;
import com.org.linkedin.dto.ApiResponse;
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

  /**
   * Creates a new role in Keycloak.
   *
   * @param requestedRole The RoleDTO containing the details of the role to be created.
   * @return A ResponseEntity containing an ApiResponse with the created RoleDTO.
   */
  //  @PreAuthorize("hasRole('SystemAdmin')")
  @PostMapping("/role")
  public ResponseEntity<ApiResponse<RoleDTO>> save(@RequestBody @Valid RoleDTO requestedRole) {
    log.debug("Enter save method :: :: requestedRole {}", requestedRole);
    RoleDTO role = keyCloakAdminService.save(requestedRole);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Success", role));
  }

  /**
   * Retrieves a paginated list of all roles from Keycloak.
   *
   * @param pageable Pagination and sorting information.
   * @return A ResponseEntity containing an ApiResponse with a paginated list of RoleDTOs.
   */
  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant')")
  @GetMapping("/role")
  public ResponseEntity<ApiResponse<List<RoleDTO>>> getAllRoles(Pageable pageable) {
    log.debug("Enter getAllRoles method ");
    Page<RoleDTO> roles = keyCloakAdminService.getAllRoles(pageable);
    HttpHeaders headers =
        PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), roles);
    return ResponseEntity.ok()
        .headers(headers)
        .body(
            ApiResponse.success(
                "Success",
                roles.getContent(),
                roles.getNumber(),
                roles.getSize(),
                roles.getTotalElements()));
  }

  /**
   * Retrieves a specific role by its unique identifier.
   *
   * @param roleId The unique identifier of the role to retrieve.
   * @return A ResponseEntity containing an ApiResponse with the requested RoleDTO.
   */
  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant')")
  @GetMapping("/role/{id}")
  public ResponseEntity<ApiResponse<RoleDTO>> getRoleById(@PathVariable("id") UUID roleId) {
    log.debug("Enter getRoleById method :: :: roleId :: [{}]", roleId);
    RoleDTO role = keyCloakAdminService.getRoleById(roleId);
    return ResponseEntity.ok(ApiResponse.success("Success", role));
  }

  /**
   * Assigns a specific role to a user in Keycloak.
   *
   * @param roleId The unique identifier of the role to assign.
   * @param userId The unique identifier of the user (Keycloak user ID) to whom the role is
   *     assigned.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @PreAuthorize("hasRole('SystemAdmin')")
  @PostMapping("/assignRole")
  public ResponseEntity<ApiResponse<Void>> assignRoleToUser(
      @RequestParam("roleId") UUID roleId, @RequestParam("keycloakUserId") UUID userId) {
    log.debug("Enter assignRoleToUser method :: :: roleId :: [{}]", roleId);
    keyCloakAdminService.assignRoleToUser(roleId, userId);
    return ResponseEntity.ok(ApiResponse.success("ROLE_ASSIGNED_SUCCESSFULLY", null));
  }

  /**
   * Deletes a specific role from Keycloak.
   *
   * @param roleId The unique identifier of the role to delete.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @PreAuthorize("hasRole('SystemAdmin')")
  @DeleteMapping("/role/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable("id") UUID roleId) {
    log.debug("Enter deleteRole method :: :: roleId :: [{}]", roleId);
    keyCloakAdminService.deleteRole(roleId);
    return ResponseEntity.ok(ApiResponse.success(Constants.OPERATION_SUCCESSFUL, null));
  }

  /**
   * Updates an existing role's details in Keycloak.
   *
   * @param roleId The unique identifier of the role to update.
   * @param requestedRole The RoleDTO containing the updated details.
   * @return A ResponseEntity containing an ApiResponse with the updated RoleDTO.
   */
  @PreAuthorize("hasRole('SystemAdmin')")
  @PutMapping("/role/{id}")
  public ResponseEntity<ApiResponse<RoleDTO>> updateRole(
      @PathVariable("id") UUID roleId, @RequestBody RoleDTO requestedRole) {
    log.debug("Enter updateRole method :: :: roleId :: [{}]", roleId);
    RoleDTO role = keyCloakAdminService.updateRole(roleId, requestedRole);
    return ResponseEntity.ok(ApiResponse.success("Success", role));
  }
}
