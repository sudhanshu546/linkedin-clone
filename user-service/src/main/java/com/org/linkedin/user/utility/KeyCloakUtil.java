package com.org.linkedin.user.utility;

import com.org.linkedin.constants.Constants;
import com.org.linkedin.dto.user.TUserDTO;
import com.org.linkedin.user.config.keycloak.BasicProperties;
import com.org.linkedin.user.config.keycloak.KeycloakClients;
import com.org.linkedin.user.config.keycloak.UserEntryBuilder;
import com.org.linkedin.user.config.keycloak.user.KeycloakDemoClientUserProperties;
import com.org.linkedin.user.mapper.PermissionMapper;
import com.org.linkedin.utility.errors.ErrorKeys;
import com.org.linkedin.utility.exception.CommonExceptionHandler;
import io.micrometer.common.util.StringUtils;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation.RoleDefinition;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

@AllArgsConstructor
@Component
@Slf4j
public class KeyCloakUtil {

  private final UserEntryBuilder userEntryBuilder;

  private final KeycloakDemoClientUserProperties keycloakDemoClientUserProperties;

  private final PermissionMapper permissionMapper;

  private final KeycloakClients keycloakClient;

  private final BasicProperties basicProperties;

  /**
   * Creates a new user in Keycloak.
   *
   * @param clientName the name of the Keycloak client.
   * @param userRepresentation the representation of the user to be created.
   * @return the response from the Keycloak server after attempting to create the user.
   */
  private Response createUserInKeycloak(String clientName, UserRepresentation userRepresentation) {
    Keycloak builder = getKeycloakEntryBuilder(clientName);
    return builder
        .realm(keycloakDemoClientUserProperties.getRealm())
        .users()
        .create(userRepresentation);
  }

  private CredentialRepresentation createPasswordCredentials(String password) {
    CredentialRepresentation passwordCredentials = new CredentialRepresentation();
    passwordCredentials.setTemporary(false);
    passwordCredentials.setType(CredentialRepresentation.PASSWORD);
    passwordCredentials.setValue(password);
    return passwordCredentials;
  }

  /**
   * Returns a Keycloak instance based on the provided client name.
   *
   * @param clientName the name of the Keycloak client.
   * @return the Keycloak instance configured for the specified client.
   */
  public Keycloak getKeycloakEntryBuilder(String clientName) {
    return clientName.equalsIgnoreCase(keycloakClient.clientName())
        ? userEntryBuilder.keycloakBuilder(keycloakDemoClientUserProperties)
        : null;
  }

  /**
   * Retrieves the client ID based on the provided client name.
   *
   * @param clientName the name of the Keycloak client.
   * @return the client ID associated with the specified client.
   */
  private String getClientId(String clientName) {
    return clientName.equals(keycloakClient.clientName()) ? keycloakClient.clientId() : null;
  }

  /**
   * Updates the password for a specified user in Keycloak.
   *
   * @param userId the ID of the user whose password is to be updated.
   * @param newPassword the new password to set for the user.
   * @param clientName the name of the Keycloak client used to determine the realm and
   *     client-specific configurations.
   * @throws ResponseStatusException if there is an error during the password update process.
   */
  public void updateUserPassword(String userId, String newPassword, String clientName) {
    try {
      Keycloak builder = getKeycloakEntryBuilder(clientName);
      CredentialRepresentation newPasswordCredentials = createPasswordCredentials(newPassword);
      builder
          .realm(keycloakDemoClientUserProperties.getRealm())
          .users()
          .get(userId)
          .resetPassword(newPasswordCredentials);

    } catch (Exception e) {
      log.error(
          "Unknown error in updateUserPassword method :: client name [{}] :: userId [{}]",
          clientName,
          userId,
          e);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Unable to update user password, please try again after some time");
    }
  }

  /**
   * Saves employee details in Keycloak and assigns a default role.
   *
   * @param request The employee data transfer object containing employee details.
   * @param clientName The name of the Keycloak client used for the operation.
   * @param roleName
   * @return The ID of the created employee in Keycloak.
   * @throws CommonExceptionHandler if the employee already exists or if there is an error during
   *     the creation process.
   */
  public String saveUser(TUserDTO request, String clientName, String roleName) {
    UserRepresentation employeeRepresentation = createEmployeeRepresentation(request);
    try {
      Response response = createUserInKeycloak(clientName, employeeRepresentation);

      if (response.getStatus() == HttpStatus.CONFLICT.value()) {
        log.debug(
            "Employee Already Exists with Email in SaveEmployee Request :: email [{}] ",
            request.getEmail());
        throw new CommonExceptionHandler(
            ErrorKeys.USER_ALREADY_EXISTS_WITH_THIS_EMAIL, HttpStatus.BAD_REQUEST.value());
      }
      String userId = CreatedResponseUtil.getCreatedId(response);
      ClientResource clientResource = getClientResource(keycloakClient);
      assignRoleToEmployee(clientResource, userId, roleName);
      return userId;
    } catch (CommonExceptionHandler e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Unknown error in keyCloakUtil.saveEmployee method :: client name [{}]", clientName, e);
      throw new CommonExceptionHandler(
          ErrorKeys.UNABLE_TO_CREATE_USER, HttpStatus.BAD_REQUEST.value());
    }
  }

  public void assignRoleToEmployee(ClientResource clientResource, String userId, String roleName) {

    RoleRepresentation role = clientResource.roles().get(roleName).toRepresentation();
    Keycloak builder = getKeycloakEntryBuilder(keycloakClient.clientName());
    builder
        .realm(keycloakDemoClientUserProperties.getRealm())
        .users()
        .get(userId)
        .roles()
        .clientLevel(keycloakClient.clientId())
        .add(Collections.singletonList(role));
  }

  private UserRepresentation createEmployeeRepresentation(TUserDTO request) {
    UserRepresentation userRepresentation = new UserRepresentation();
    userRepresentation.setEmail(request.getEmail());
    userRepresentation.setFirstName(request.getFirstName());
    userRepresentation.setLastName(request.getLastName());
    userRepresentation.setEnabled(true);
    CredentialRepresentation passwordCredentials = createPasswordCredentials(request.getPassword());
    userRepresentation.setCredentials(List.of(passwordCredentials));
    userRepresentation.setEmailVerified(false);
    return userRepresentation;
  }

  /**
   * Deletes a user from Keycloak.
   *
   * <p>This method attempts to delete a user identified by the provided UserId from the Keycloak
   * server configured for the specified client. It uses the client name to determine the
   * appropriate Keycloak configuration and realm.
   *
   * @param clientName the name of the Keycloak client used to determine the realm and
   *     client-specific configurations.
   * @param UserId the unique identifier of the user to be deleted.
   * @return the response from the Keycloak server after attempting to delete the user.
   * @throws CommonExceptionHandler with {@link ErrorKeys#USER_NOT_FOUND} if the user does not
   *     exist.
   * @throws CommonExceptionHandler with {@link ErrorKeys#SYSTEM_ERROR} for any other errors
   *     encountered during the operation.
   */
  public Response deleteUserFromKeycloak(String clientName, String UserId) {
    Keycloak builder = getKeycloakEntryBuilder(clientName);
    try {
      builder
          .realm(keycloakDemoClientUserProperties.getRealm())
          .users()
          .get(UserId)
          .toRepresentation();
      Response response =
          builder.realm(keycloakDemoClientUserProperties.getRealm()).users().delete(UserId);
      return response;
    } catch (NotFoundException ex) {
      System.out.println("User not found: ");
      throw new CommonExceptionHandler(ErrorKeys.USER_NOT_FOUND, HttpStatus.BAD_REQUEST.value());
    } catch (Exception e) {
      e.printStackTrace();
      throw new CommonExceptionHandler(ErrorKeys.SYSTEM_ERROR, HttpStatus.BAD_REQUEST.value());
    }
  }

  /**
   * Disables a user in Keycloak by setting the user's enabled status to false.
   *
   * @param clientName the name of the Keycloak client used to determine the realm and
   *     client-specific configurations.
   * @param userId the unique identifier of the user to be disabled.
   * @throws CommonExceptionHandler with {@link ErrorKeys#ERROR_DISABLING_USER} if there is an error
   *     during the disabling process.
   */
  @Async
  public void disableUserInKeycloak(String clientName, String userId) {
    Keycloak builder = getKeycloakEntryBuilder(clientName);

    try {
      UserResource userResource =
          builder.realm(keycloakDemoClientUserProperties.getRealm()).users().get(userId);

      UserRepresentation userDetails = userResource.toRepresentation();
      userDetails.setEnabled(false);
      userResource.update(userDetails);
      log.info("User Disabled successfully.");
    } catch (Exception e) {
      throw new CommonExceptionHandler(
          ErrorKeys.ERROR_DISABLING_USER, HttpStatus.BAD_REQUEST.value());
    }
  }

  /**
   * Enables a user in Keycloak by setting the user's enabled status to true.
   *
   * @param clientName the name of the Keycloak client used to determine the realm and
   *     client-specific configurations.
   * @param userId the unique identifier of the user to be enabled.
   * @throws CommonExceptionHandler with {@link ErrorKeys#ERROR_ENABLING_USER} if there is an error
   *     during the enabling process.
   */
  public void enableUserInKeycloak(String clientName, String userId) {
    Keycloak builder = getKeycloakEntryBuilder(clientName);

    try {
      UserResource userResource =
          builder.realm(keycloakDemoClientUserProperties.getRealm()).users().get(userId);

      UserRepresentation userDetails = userResource.toRepresentation();
      userDetails.setEnabled(true);
      userResource.update(userDetails);

      log.info("User Enabled successfully.");
    } catch (Exception e) {
      throw new CommonExceptionHandler(
          ErrorKeys.ERROR_ENABLING_USER, HttpStatus.BAD_REQUEST.value());
    }
  }

  /**
   * Checks if a user is enabled in Keycloak.
   *
   * <p>This method retrieves the user's status from Keycloak based on the provided client name and
   * user ID. It returns true if the user is enabled, otherwise false.
   *
   * @param clientName the name of the Keycloak client used to determine the realm and
   *     client-specific configurations.
   * @param userId the unique identifier of the user whose status is being checked.
   * @return true if the user is enabled, false otherwise.
   * @throws Exception if there is an error during the process of fetching the user's status.
   */
  public boolean isUserEnabledInKeycloak(String clientName, String userId) {
    Keycloak builder = getKeycloakEntryBuilder(clientName);
    try {
      UserResource userResource =
          builder.realm(keycloakDemoClientUserProperties.getRealm()).users().get(userId);
      UserRepresentation userDetails = userResource.toRepresentation();
      return userDetails.isEnabled();
    } catch (Exception e) {
      log.error("Error checking user status in Keycloak", e);
      return false;
    }
  }

  /**
   * Updates the email address of a specified user in Keycloak.
   *
   * @param userId the ID of the user whose email is to be updated.
   * @param newEmail the new email address to set for the user.
   * @param clientName the name of the Keycloak client used to determine the realm and
   *     client-specific configurations.
   * @throws ResponseStatusException if there is an error during the email update process.
   */
  public void updateUserEmail(String userId, String newEmail, String clientName) {
    try {
      Keycloak builder = getKeycloakEntryBuilder(clientName);
      UserResource userResource =
          builder.realm(keycloakDemoClientUserProperties.getRealm()).users().get(userId);

      UserRepresentation user = userResource.toRepresentation();
      user.setEmail(newEmail);
      userResource.update(user);

    } catch (Exception e) {
      log.error(
          "Unknown error in updateUserEmail method :: client name [{}] :: userId [{}]",
          clientName,
          userId,
          e);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unable to update user email, please try again after some time");
    }
  }

  public List<ResourceRepresentation> getResources() {
    List<ResourceRepresentation> resources = new ArrayList<ResourceRepresentation>();
    try {
      Keycloak builder = getKeycloakEntryBuilder(keycloakClient.clientName());
      List<ResourceRepresentation> staffResources =
          builder
              .realm(keycloakDemoClientUserProperties.getRealm())
              .clients()
              .get(keycloakClient.clientId())
              .authorization()
              .resources()
              .resources();
      if (!CollectionUtils.isEmpty(staffResources)) {
        resources.addAll(staffResources);
      }
      return resources;
    } catch (Exception e) {
      log.error("Error while fetching the :: [{}]", keycloakClient.clientName(), " resources");
      throw e;
    }
  }

  public List<ResourceRepresentation> getResourcesByRoleId(
      String roleId, ClientResource clientResource) {

    List<PolicyRepresentation> filteredRolePolicies =
        filterPoliciesByRoleId(clientResource, roleId);
    // fetch dependent permissions on these policies
    List<PolicyRepresentation> fetchPolicyPermissions =
        fetchPolicyPermission(clientResource, filteredRolePolicies);
    List<ResourceRepresentation> requiredResources = new ArrayList<>();
    for (PolicyRepresentation permission : fetchPolicyPermissions) {
      String permissionId = permission.getId();
      List<ResourceRepresentation> resources =
          clientResource.authorization().policies().policy(permission.getId()).resources().stream()
              .toList();
      boolean doesResourceExist = false;
      for (ResourceRepresentation resource : requiredResources) {
        for (ResourceRepresentation fetchedResource : resources) {
          if (fetchedResource.getId().equals(resource.getId())) {
            doesResourceExist = true;
            adjustScopeToExistingResource(resource, clientResource, permissionId);
          }
        }
        if (doesResourceExist) {
          break;
        }
      }
      if (!doesResourceExist) {
        resources =
            resources.stream()
                .map(
                    (resource) -> {
                      Set<ScopeRepresentation> scopes =
                          clientResource
                              .authorization()
                              .policies()
                              .policy(permission.getId())
                              .scopes()
                              .stream()
                              .collect(Collectors.toSet());
                      resource.setScopes(scopes);
                      return resource;
                    })
                .toList();
        requiredResources.addAll(resources);
      }
    }
    return requiredResources;
  }

  private void adjustScopeToExistingResource(
      ResourceRepresentation resource, ClientResource clientResource, String permissionId) {
    Set<ScopeRepresentation> scopes =
        clientResource.authorization().policies().policy(permissionId).scopes().stream()
            .collect(Collectors.toSet());

    Set<ScopeRepresentation> existingScopes = resource.getScopes();

    if (!CollectionUtils.isEmpty(existingScopes) && !CollectionUtils.isEmpty(scopes)) {
      existingScopes = new HashSet<>(existingScopes);
      existingScopes.addAll(scopes);
      resource.setScopes(existingScopes);
    } else {
      resource.setScopes(scopes);
    }
  }

  private List<PolicyRepresentation> fetchPolicyPermission(
      ClientResource clientResource, List<PolicyRepresentation> filteredRolePolicies) {
    List<PolicyRepresentation> permissions = new ArrayList<PolicyRepresentation>();
    filteredRolePolicies.stream()
        .forEach(
            (per) -> {
              permissions.addAll(
                  clientResource
                      .authorization()
                      .policies()
                      .policy(per.getId())
                      .dependentPolicies());
            });
    return permissions;
  }

  private List<PolicyRepresentation> filterPoliciesByRoleId(
      ClientResource clientResource, String roleId) {
    List<PolicyRepresentation> rolePolicies =
        clientResource
            .authorization()
            .policies()
            .policies(null, null, null, null, null, false, null, null, null, null);
    List<PolicyRepresentation> filteredRolePolicies =
        rolePolicies.stream()
            .filter(
                rolePolicy -> {
                  Set<RoleDefinition> roles =
                      clientResource
                          .authorization()
                          .policies()
                          .role()
                          .findById(rolePolicy.getId())
                          .toRepresentation()
                          .getRoles();
                  // Ensure roles is not null and not empty before checking
                  return roles != null
                      && !roles.isEmpty()
                      && roles.stream().anyMatch(role -> role.getId().equals(roleId));
                })
            .collect(Collectors.toList());
    return filteredRolePolicies;
  }

  public RoleRepresentation addRoleToKeyCloak(
      ClientResource clientResource, RoleRepresentation requestedRole) {
    clientResource.roles().create(requestedRole);
    RoleRepresentation role =
        clientResource.roles().get(requestedRole.getName()).toRepresentation();
    return role;
  }

  public ClientResource getClientResource(KeycloakClients client) {
    Keycloak builder = getKeycloakEntryBuilder(client.clientName());

    String realmName = getRealmName(client);
    ClientResource clientResource = builder.realm(realmName).clients().get(client.clientId());
    return clientResource;
  }

  private String getRealmName(KeycloakClients client) {
    return client == keycloakClient ? keycloakDemoClientUserProperties.getRealm() : null;
  }

  public PolicyRepresentation createPolicyByRole(
      ClientResource clientResource, RoleRepresentation role) {
    RolePolicyRepresentation policy = new RolePolicyRepresentation();
    policy.setName(Constants.POLICY_PREFIX + role.getName());
    Set<RoleDefinition> roleDefinition = Set.of(new RoleDefinition(role.getId(), true));
    policy.setRoles(roleDefinition);
    clientResource.authorization().policies().role().create(policy);
    PolicyRepresentation policyRepresentation =
        clientResource
            .authorization()
            .policies()
            .findByName(Constants.POLICY_PREFIX + role.getName());
    return policyRepresentation;
  }

  public PolicyRepresentation getPolicyByRole(
      ClientResource clientResource, RoleRepresentation role) {
    PolicyRepresentation policyRepresentation =
        clientResource
            .authorization()
            .policies()
            .findByName(Constants.POLICY_PREFIX + role.getName());
    if (policyRepresentation == null) {
      policyRepresentation = createPolicyByRole(clientResource, role);
    }
    return policyRepresentation;
  }

  public RolePolicyRepresentation getPolicyByRole(
      ClientResource clientResource, String keyCloakRoleName) {
    RolePolicyRepresentation policyRepresentation =
        clientResource
            .authorization()
            .policies()
            .role()
            .findByName(Constants.POLICY_PREFIX + keyCloakRoleName);
    return policyRepresentation;
  }

  public RoleRepresentation fetchRoleFromId(ClientResource clientResource, String roleId) {
    List<RoleRepresentation> roles = clientResource.roles().list();
    Optional<RoleRepresentation> roleOptional =
        roles.stream().filter((role) -> role.getId().equals(roleId)).findFirst();
    if (roleOptional.isEmpty()) {
      throw new CommonExceptionHandler(ErrorKeys.INVALID_ROLE, HttpStatus.BAD_REQUEST.value());
    }
    RoleRepresentation role = roleOptional.get();
    return role;
  }

  public boolean validateResourceRequestAndFetchType(
      ResourceRepresentation resource, ClientResource clientResource) {
    boolean isPermissionRequestScope = false;
    if (ObjectUtils.isNotEmpty(resource)) {
      Set<ScopeRepresentation> scopes = resource.getScopes();
      if (!CollectionUtils.isEmpty(scopes)) {
        scopes.stream().forEach((scope) -> fetchScopeById(scope.getId(), clientResource));
        isPermissionRequestScope = true;
      }
    }
    return isPermissionRequestScope;
  }

  public ResourceRepresentation processResource(
      Set<ResourceRepresentation> resources, ClientResource clientResource) {
    ResourceRepresentation resource = null;
    if (!CollectionUtils.isEmpty(resources)) {
      resource = CollectionUtils.firstElement(resources);
      if (StringUtils.isNotBlank(resource.getId())) {
        resource = fetchResourceById(resource.getId(), clientResource);
      }
    }
    return resource;
  }

  private ResourceRepresentation fetchResourceById(
      String resourceId, ClientResource clientResource) {
    ResourceRepresentation resourceRepresentation =
        clientResource.authorization().resources().resource(resourceId).toRepresentation();
    if (resourceRepresentation == null) {
      throw new CommonExceptionHandler(
          ErrorKeys.RESOURCE_NOT_FOUND, HttpStatus.BAD_REQUEST.value());
    }
    return resourceRepresentation;
  }

  private ScopeRepresentation fetchScopeById(String scopeId, ClientResource clientResource) {
    ScopeRepresentation scopeRepresentation =
        clientResource.authorization().scopes().scope(scopeId).toRepresentation();
    if (scopeRepresentation == null) {
      throw new CommonExceptionHandler(ErrorKeys.SCOPE_NOT_FOUND, HttpStatus.BAD_REQUEST.value());
    }
    return scopeRepresentation;
  }

  public Page<RoleRepresentation> getAllRoles(ClientResource clientResource, Pageable pageable) {
    long totalRoles = clientResource.roles().list().size();
    int firstRecords = (int) pageable.getOffset();
    int maxRecords = pageable.getPageSize();
    List<RoleRepresentation> roles = clientResource.roles().list(firstRecords, maxRecords, true);
    return new PageImpl<RoleRepresentation>(roles, pageable, totalRoles);
  }

  public RoleRepresentation updateRole(
      ClientResource clientResource,
      String previousKeyCloackRoleName,
      RoleRepresentation keyCloackRole) {
    clientResource.roles().get(previousKeyCloackRoleName).update(keyCloackRole);
    keyCloackRole = fetchRoleFromId(clientResource, keyCloackRole.getId());
    return keyCloackRole;
  }

  public void deleteRole(ClientResource clientResource, String roleId) {
    RoleRepresentation role = fetchRoleFromId(clientResource, roleId);
    clientResource.roles().deleteRole(role.getName());
  }

  public AbstractPolicyRepresentation validateAndFetchPermission(
      String permissionId, ClientResource clientResource) {
    AbstractPolicyRepresentation policy = null;
    try {
      policy = clientResource.authorization().policies().policy(permissionId).toRepresentation();
      if (ObjectUtils.isEmpty(policy)) {
        throw new CommonExceptionHandler(
            ErrorKeys.PERMISSION_NOT_FOUND, HttpStatus.BAD_REQUEST.value());
      }
    } catch (Throwable e) {
      throw new CommonExceptionHandler(
          ErrorKeys.PERMISSION_NOT_FOUND, HttpStatus.BAD_REQUEST.value());
    }
    return policy;
  }

  public void fetchPermissionWithResourcesAndScopes(
      ClientResource clientResource, String permissionId, AbstractPolicyRepresentation permission) {
    List<ResourceRepresentation> resources =
        clientResource.authorization().policies().policy(permissionId).resources();
    if (!CollectionUtils.isEmpty(resources)) {
      permission.setResourcesData(resources.stream().collect(Collectors.toSet()));
    }
    List<ScopeRepresentation> scopes =
        clientResource.authorization().policies().policy(permissionId).scopes();
    if (!CollectionUtils.isEmpty(scopes)) {
      permission.setScopesData(scopes.stream().collect(Collectors.toSet()));
    }
  }

  public void verifyUniquePermissionName(
      ClientResource clientResource, AbstractPolicyRepresentation permission) {
    permission =
        clientResource
            .authorization()
            .policies()
            .findByName(Constants.PERMISSION_PREFIX + permission.getName());
    if (permission != null) {
      throw new CommonExceptionHandler(
          ErrorKeys.PERMISSION_EXISTS_WITH_SAME_NAME, HttpStatus.BAD_REQUEST.value());
    }
  }

  public void updatePolicyByRole(
      ClientResource clientResource, String updatedRoleName, RolePolicyRepresentation policy) {
    policy.setName(Constants.POLICY_PREFIX + updatedRoleName);
    clientResource.authorization().policies().role().findById(policy.getId()).update(policy);
  }

  public void deletePolicyById(ClientResource clientResource, String policyId) {
    clientResource
        .authorization()
        .policies()
        .role()
        .findById(Constants.POLICY_PREFIX + policyId)
        .remove();
  }

  public void updatePermissionsByRoleId(
      List<ResourceRepresentation> roleResources,
      ClientResource clientResource,
      List<ResourceRepresentation> updatedResources,
      String keyCloackRoleId) {
    // fetch dependent permissions on these policies
    List<ResourceRepresentation> removedResources =
        fetchRemovedResources(roleResources, updatedResources);
    List<PolicyRepresentation> policies = filterPoliciesByRoleId(clientResource, keyCloackRoleId);
    deletePoliciesFromPermission(removedResources, policies, clientResource);
    List<ResourceRepresentation> resourcesToBeAdded =
        fetchUpdatedResources(roleResources, updatedResources);
    addPoliciesToPermission(resourcesToBeAdded, policies, clientResource);
  }

  private void addPoliciesToPermission(
      List<ResourceRepresentation> resourcesToBeAdded,
      List<PolicyRepresentation> policies,
      ClientResource clientResource) {
    Set<String> policyIdsToBeAdded =
        policies.stream().map(policy -> policy.getId()).collect(Collectors.toSet());
    for (ResourceRepresentation resource : resourcesToBeAdded) {
      Set<ScopeRepresentation> resourceScopes = new HashSet<>(resource.getScopes());
      List<PolicyRepresentation> resourcePermissions =
          clientResource.authorization().resources().resource(resource.getId()).permissions();
      if (CollectionUtils.isEmpty(resourcePermissions)) {
        // add permissions by resources and scopes
        addPermissionsByResourceAndScopes(
            clientResource, resource, resourceScopes, policyIdsToBeAdded);
      } else {
        // permission exist for resource
        Set<ScopeRepresentation> scopePermissionsToBeUpdated = new HashSet<>();
        for (PolicyRepresentation permission : resourcePermissions) {
          List<ScopeRepresentation> permissionScopes =
              clientResource.authorization().policies().policy(permission.getId()).scopes();
          if (CollectionUtils.isEmpty(permissionScopes)) {
            // update existing rescource based permission
            updateResourceBasedPermissionByPolicies(
                policyIdsToBeAdded, permission, resource, permissionScopes, clientResource);
          }
          boolean doesScopePermissionExists = false;
          for (ScopeRepresentation scope : resourceScopes) {
            for (ScopeRepresentation permissionScope : permissionScopes) {
              if (scope.getId().equals(permissionScope.getId())) {
                // update the permission with given policies
                updateScopeBasedPermissionByPolicies(
                    policyIdsToBeAdded, permission, resource, permissionScopes, clientResource);
                doesScopePermissionExists = true;
                scopePermissionsToBeUpdated.add(scope);
                break;
              }
            }
            if (doesScopePermissionExists) {
              break;
            }
          }
        }
        // need to create scope based permissions for the left scopes for resource
        resourceScopes.removeAll(scopePermissionsToBeUpdated);
        addScopeBasedPermissions(clientResource, resource, resourceScopes, policyIdsToBeAdded);
      }
    }
  }

  private void updateResourceBasedPermissionByPolicies(
      Set<String> policyIdsToBeAdded,
      PolicyRepresentation permission,
      ResourceRepresentation resource,
      List<ScopeRepresentation> permissionScopes,
      ClientResource clientResource) {
    Set<String> linkedPolicies =
        clientResource
            .authorization()
            .policies()
            .policy(permission.getId())
            .associatedPolicies()
            .stream()
            .map(policy -> policy.getId())
            .collect(Collectors.toSet());
    linkedPolicies.addAll(policyIdsToBeAdded);
    ResourcePermissionRepresentation resourcePermissionRepresentation =
        permissionMapper.toResourcePermission(permission);
    resourcePermissionRepresentation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
    resourcePermissionRepresentation.setResources(Set.of(resource.getId()));
    resourcePermissionRepresentation.setPolicies(linkedPolicies);
    clientResource
        .authorization()
        .permissions()
        .resource()
        .findById(permission.getId())
        .update(resourcePermissionRepresentation);
  }

  private void updateScopeBasedPermissionByPolicies(
      Set<String> policyIdsToBeAdded,
      PolicyRepresentation permission,
      ResourceRepresentation resource,
      List<ScopeRepresentation> permissionScopes,
      ClientResource clientResource) {
    Set<String> linkedPolicies =
        clientResource
            .authorization()
            .policies()
            .policy(permission.getId())
            .associatedPolicies()
            .stream()
            .map(policy -> policy.getId())
            .collect(Collectors.toSet());
    linkedPolicies.addAll(policyIdsToBeAdded);
    ScopePermissionRepresentation scopePermissionRepresentation =
        permissionMapper.toScopePermission(permission);
    scopePermissionRepresentation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
    scopePermissionRepresentation.setResources(Set.of(resource.getId()));
    scopePermissionRepresentation.setPolicies(linkedPolicies);
    clientResource
        .authorization()
        .permissions()
        .scope()
        .findById(permission.getId())
        .update(scopePermissionRepresentation);
  }

  private void addPermissionsByResourceAndScopes(
      ClientResource clientResource,
      ResourceRepresentation resource,
      Set<ScopeRepresentation> scopes,
      Set<String> policyIdsToBeAdded) {

    if (!CollectionUtils.isEmpty(scopes)) {
      addScopeBasedPermissions(clientResource, resource, scopes, policyIdsToBeAdded);
    } else {
      addResourceBasedPermission(clientResource, resource, scopes, policyIdsToBeAdded);
    }
  }

  private void addScopeBasedPermissions(
      ClientResource clientResource,
      ResourceRepresentation resource,
      Set<ScopeRepresentation> scopes,
      Set<String> policyIdsToBeAdded) {
    // scope based permission
    for (ScopeRepresentation scope : scopes) {
      scope = clientResource.authorization().scopes().scope(scope.getId()).toRepresentation();
      String permissionName =
          Constants.PERMISSION_PREFIX + resource.getName() + "_" + scope.getName();
      ScopePermissionRepresentation scopePermissionRepresentation =
          new ScopePermissionRepresentation();
      scopePermissionRepresentation.setName(permissionName);
      scopePermissionRepresentation.setPolicies(policyIdsToBeAdded);
      scopePermissionRepresentation.setResources(Set.of(resource.getId()));
      scopePermissionRepresentation.setScopes(Set.of(scope.getId()));
      scopePermissionRepresentation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
      clientResource.authorization().permissions().scope().create(scopePermissionRepresentation);
    }
  }

  private void addResourceBasedPermission(
      ClientResource clientResource,
      ResourceRepresentation resource,
      Set<ScopeRepresentation> scopes,
      Set<String> policyIdsToBeAdded) {
    // resource based
    String permissionName = Constants.PERMISSION_PREFIX + resource.getName();
    ResourcePermissionRepresentation resourcePermissionRepresentation =
        new ResourcePermissionRepresentation();
    resourcePermissionRepresentation.setName(permissionName);
    resourcePermissionRepresentation.setPolicies(policyIdsToBeAdded);
    resourcePermissionRepresentation.setResources(Set.of(resource.getId()));
    resourcePermissionRepresentation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
    clientResource
        .authorization()
        .permissions()
        .resource()
        .create(resourcePermissionRepresentation);
  }

  private List<ResourceRepresentation> fetchUpdatedResources(
      List<ResourceRepresentation> roleResources, List<ResourceRepresentation> updatedResources) {
    List<ResourceRepresentation> modifiedResources = new ArrayList<>();
    for (ResourceRepresentation updatedResource : updatedResources) {
      boolean isNewResource = true;
      for (ResourceRepresentation resource : roleResources) {
        if (resource.getId().equals(updatedResource.getId())) {
          ResourceRepresentation newResource = new ResourceRepresentation();
          newResource.setId(updatedResource.getId());
          Set<ScopeRepresentation> resourceScopes = resource.getScopes();
          Set<ScopeRepresentation> updatedScopes = updatedResource.getScopes();
          Set<ScopeRepresentation> newScopes = new HashSet<>();
          for (ScopeRepresentation updatedScope : updatedScopes) {
            boolean isNewScope = true;
            for (ScopeRepresentation resourceScope : resourceScopes) {
              if (resourceScope.getId().equals(updatedScope.getId())) {
                isNewScope = false;
                break;
              }
            }
            if (isNewScope) {
              newScopes.add(updatedScope);
            }
          }
          if (CollectionUtils.isEmpty(newScopes)) {
            isNewResource = false;
            break;
          }
          newResource.setScopes(newScopes);
          resource = newResource;
          break;
        }
      }
      if (isNewResource) {
        modifiedResources.add(updatedResource);
      }
    }
    return modifiedResources;
  }

  private void deletePoliciesFromPermission(
      List<ResourceRepresentation> removedResources,
      List<PolicyRepresentation> policies,
      ClientResource clientResource) {

    Set<String> policyIdsToBeRemoved =
        policies.stream().map(policy -> policy.getId()).collect(Collectors.toSet());
    for (ResourceRepresentation resource : removedResources) {
      Set<ScopeRepresentation> scopes = resource.getScopes();
      List<PolicyRepresentation> resourcePermissions =
          clientResource.authorization().resources().resource(resource.getId()).permissions();
      for (PolicyRepresentation permission : resourcePermissions) {
        if (CollectionUtils.isEmpty(scopes)) {
          removePoliciesFromResourcePermission(
              policyIdsToBeRemoved, permission, resource, clientResource);
          break;
        }
        List<ScopeRepresentation> permissionScopes =
            clientResource.authorization().policies().policy(permission.getId()).scopes();
        for (ScopeRepresentation permissionScope : permissionScopes) {
          for (ScopeRepresentation scope : scopes) {
            if (scope.getId().equals(permissionScope.getId())) {
              // remove the policies from this permission
              removePoliciesFromScopePermission(
                  policyIdsToBeRemoved, permission, resource, permissionScopes, clientResource);
              break;
            }
          }
        }
      }
    }
  }

  private void removePoliciesFromResourcePermission(
      Set<String> policyIdsToBeRemoved,
      PolicyRepresentation permission,
      ResourceRepresentation resource,
      ClientResource clientResource) {
    Set<String> linkedPolicies =
        clientResource
            .authorization()
            .policies()
            .policy(permission.getId())
            .associatedPolicies()
            .stream()
            .map(policy -> policy.getId())
            .collect(Collectors.toSet());
    linkedPolicies.removeAll(policyIdsToBeRemoved);
    ResourcePermissionRepresentation resourcePermissionRepresentation =
        permissionMapper.toResourcePermission(permission);
    resourcePermissionRepresentation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
    resourcePermissionRepresentation.setResources(Set.of(resource.getId()));
    resourcePermissionRepresentation.setPolicies(linkedPolicies);
    clientResource
        .authorization()
        .permissions()
        .resource()
        .findById(permission.getId())
        .update(resourcePermissionRepresentation);
  }

  private void removePoliciesFromScopePermission(
      Set<String> policyIdsToBeRemoved,
      PolicyRepresentation permission,
      ResourceRepresentation resource,
      List<ScopeRepresentation> permissionScopes,
      ClientResource clientResource) {
    Set<String> linkedPolicies =
        clientResource
            .authorization()
            .policies()
            .policy(permission.getId())
            .associatedPolicies()
            .stream()
            .map(policy -> policy.getId())
            .collect(Collectors.toSet());
    linkedPolicies.removeAll(policyIdsToBeRemoved);
    ScopePermissionRepresentation scopePermissionRepresentation =
        permissionMapper.toScopePermission(permission);
    scopePermissionRepresentation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
    scopePermissionRepresentation.setResources(Set.of(resource.getId()));
    scopePermissionRepresentation.setScopes(
        permissionScopes.stream().map(scope -> scope.getId()).collect(Collectors.toSet()));
    scopePermissionRepresentation.setPolicies(linkedPolicies);
    clientResource
        .authorization()
        .permissions()
        .scope()
        .findById(permission.getId())
        .update(scopePermissionRepresentation);
  }

  private List<ResourceRepresentation> fetchRemovedResources(
      List<ResourceRepresentation> roleResources, List<ResourceRepresentation> updatedResources) {

    List<ResourceRepresentation> removedResources = new ArrayList<>();
    for (ResourceRepresentation resource : roleResources) {
      boolean isResourceDeleted = true;
      for (ResourceRepresentation updatedResource : updatedResources) {
        if (resource.getId().equals(updatedResource.getId())) {
          ResourceRepresentation deletedResource = new ResourceRepresentation();
          deletedResource.setId(updatedResource.getId());
          Set<ScopeRepresentation> roleScopes = resource.getScopes();
          Set<ScopeRepresentation> updatedScopes = updatedResource.getScopes();
          Set<ScopeRepresentation> deletedScopes = new HashSet<>();
          for (ScopeRepresentation roleScope : roleScopes) {
            boolean isScopeDeleted = true;
            for (ScopeRepresentation updatedScope : updatedScopes) {
              if (roleScope.getId().equals(updatedScope.getId())) {
                isScopeDeleted = false;
                break;
              }
            }
            if (isScopeDeleted) {
              deletedScopes.add(roleScope);
            }
          }
          if (CollectionUtils.isEmpty(deletedScopes)) {
            isResourceDeleted = false;
            break;
          }
          deletedResource.setScopes(deletedScopes);
          resource = deletedResource;
          break;
        }
      }
      if (isResourceDeleted) {
        removedResources.add(resource);
      }
    }
    return removedResources;
  }

  public void updateUserDetailsInKeycloak(TUserDTO updatedEmployeeDTO, KeycloakClients client) {
    Keycloak keycloak = getKeycloakEntryBuilder(client.clientName());
    String realmName = getRealmName(client);
    String employeeKeycloakId = updatedEmployeeDTO.getKeycloakUserId().toString();

    try {
      UserRepresentation userRepresentation =
          keycloak.realm(realmName).users().get(employeeKeycloakId).toRepresentation();

      userRepresentation.setFirstName(updatedEmployeeDTO.getFirstName());
      userRepresentation.setLastName(updatedEmployeeDTO.getLastName());
      keycloak.realm(realmName).users().get(employeeKeycloakId).update(userRepresentation);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
