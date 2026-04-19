package com.org.linkedin.user.controller;

import static com.org.linkedin.utility.CommonConstants.SUCCESS;

import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.user.ChangePassword;
import com.org.linkedin.dto.user.PrivacySettingsDTO;
import com.org.linkedin.dto.user.TUserDTO;
import com.org.linkedin.user.config.keycloak.KeycloakClients;
import com.org.linkedin.user.service.UserService;
import com.org.linkedin.utility.exception.ValidationException;
import com.org.linkedin.utility.service.AdvanceSearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Consolidated User Controller */
@RestController
@RequestMapping("${apiPrefix}/user")
@Slf4j
@AllArgsConstructor
@Validated
public class UserController {

  private final UserService userService;
  private final KeycloakClients keycloakClient;

  /**
   * Register a new user.
   *
   * @param userDTO the user data transfer object containing user details
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} indicating success or
   *     failure
   */
  @PostMapping("/add")
  public ResponseEntity<ApiResponse<Void>> createUser(@Valid @RequestBody TUserDTO userDTO) {
    log.debug("Enter createUser method :: [{}]", userDTO);
    userService.save(userDTO, keycloakClient.clientName());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("User created successfully.", null));
  }

  /**
   * Update user details with optional profile image.
   *
   * @param image the profile image file (optional)
   * @param userDTO the user data transfer object containing updated user details
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} with a success message
   * @throws IOException if an error occurs during image processing
   */
  @Operation(summary = "Update user profile details and/or image")
  @PutMapping("/update")
  public ResponseEntity<ApiResponse<String>> updateUser(
      @RequestParam(value = "img", required = false) MultipartFile image,
      @ModelAttribute TUserDTO userDTO)
      throws IOException {
    log.debug("Enter updateUser method:: userDTO [{}]", userDTO);
    userService.updateUserById(userDTO.getId(), image, userDTO);
    return ResponseEntity.ok(ApiResponse.success("User Updated successfully", null));
  }

  /**
   * Get details of the currently authenticated user.
   *
   * @param authentication the authentication object containing the user's principal
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} with the authenticated
   *     user's details
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<TUserDTO>> getAuthenticatedUser(Authentication authentication) {
    log.debug("Enter getAuthenticatedUser method.");
    TUserDTO userDetails = userService.getUserDetailsByAuthentication(authentication);
    return ResponseEntity.ok(ApiResponse.success(SUCCESS, userDetails));
  }

  @GetMapping("/bulk")
  public ResponseEntity<ApiResponse<List<TUserDTO>>> getUsersByIds(@RequestParam List<UUID> ids) {
    log.debug("Enter getUsersByIds method :: ids [{}]", ids);
    List<TUserDTO> users = userService.getUsersByIds(ids);
    return ResponseEntity.ok(ApiResponse.success(SUCCESS, users));
  }

  /**
   * Get any user by their ID (handles both internal UUID and Keycloak UUID via service logic).
   * Alias /user/{id} added for backward compatibility with Feign clients.
   *
   * @param id the unique identifier of the user (UUID)
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} with the user's details
   */
  @GetMapping({"/{id}", "/user/{id}"})
  public ResponseEntity<ApiResponse<TUserDTO>> getUser(@PathVariable UUID id) {
    log.debug("Enter getUser method :: id [{}]", id);
    TUserDTO userDTO =
        userService.findOne(id).orElseGet(() -> userService.findUserByKeyCloakId(id));

    return ResponseEntity.ok(ApiResponse.success(SUCCESS, userDTO));
  }

  /**
   * Delete a user.
   *
   * @param id the unique identifier of the user to be deleted
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} indicating success
   */
  //  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant')")
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
    log.debug("Enter delete method :: id [{}]", id);
    userService.delete(keycloakClient.clientName(), id);
    return ResponseEntity.ok(ApiResponse.success("Data deleted successfully", null));
  }

  /**
   * Search users by name or email.
   *
   * @param query the search query string (name or email)
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} with the list of matching
   *     users
   */
  @GetMapping("/search")
  public ResponseEntity<ApiResponse<List<TUserDTO>>> searchUsers(@RequestParam String query) {
    List<TUserDTO> users = userService.searchUsers(query);
    return ResponseEntity.ok(ApiResponse.success(SUCCESS, users));
  }

  /**
   * Advanced Search users using Criteria pattern.
   *
   * @param criteria the search criteria object
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} with the paginated list of
   *     matching users
   */
  @PostMapping("/advanced-search")
  public ResponseEntity<ApiResponse<List<TUserDTO>>> advancedSearch(
      @RequestBody AdvanceSearchCriteria criteria) {
    org.springframework.data.domain.Page<TUserDTO> result = userService.advancedSearch(criteria);
    return ResponseEntity.ok(
        ApiResponse.success(
            SUCCESS,
            result.getContent(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements()));
  }

  /**
   * Paginated list of all users.
   *
   * @param pageable the pagination information
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} with the paginated list of
   *     users
   */
  //  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant')")
  @GetMapping("/getAllUserDetail")
  public ResponseEntity<ApiResponse<List<TUserDTO>>> getAllUserDetail(Pageable pageable) {
    Page<TUserDTO> userDetails = userService.getAllUserDetail(pageable);
    return ResponseEntity.ok(
        ApiResponse.success(
            SUCCESS,
            userDetails.getContent(),
            userDetails.getNumber(),
            userDetails.getSize(),
            userDetails.getTotalElements()));
  }

  /**
   * Reset password for authenticated user.
   *
   * @param authentication the authentication object containing the user's principal
   * @param changePassword the object containing old and new password details
   * @param bindingResult the result of the validation of the changePassword object
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} with a success message
   */
  @PutMapping("/reset-password")
  public ResponseEntity<ApiResponse<String>> resetPassword(
      Authentication authentication,
      @RequestBody @Valid ChangePassword changePassword,
      BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      String errors =
          bindingResult.getAllErrors().stream()
              .map(ObjectError::getDefaultMessage)
              .collect(Collectors.joining(", "));
      throw new ValidationException(errors);
    }
    String clientName = (((Jwt) authentication.getPrincipal()).getClaims()).get("azp").toString();
    userService.resetPassword(changePassword, clientName);
    return ResponseEntity.ok(ApiResponse.success("Password Updated successfully", null));
  }

  /**
   * Request forgot password link.
   *
   * @param email the email address of the user who forgot their password
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} with the forgot password
   *     link
   * @throws Exception if an error occurs during link generation or email sending
   */
  @GetMapping("/getForgotLink")
  public ResponseEntity<ApiResponse<String>> sendForgotLink(@RequestParam String email)
      throws Exception {
    String forgotlink = userService.sendForgotLink(email);
    return ResponseEntity.ok(ApiResponse.success("Link Sent Successfully on Mail", forgotlink));
  }

  /**
   * Process forgot password token.
   *
   * @param token the forgot password token received via email
   * @param changePassword the object containing the new password details
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} with a success message
   * @throws Exception if an error occurs during password update
   */
  @PostMapping("/forgotPassword")
  public ResponseEntity<ApiResponse<String>> forgotPassword(
      @RequestParam String token, @RequestBody ChangePassword changePassword) throws Exception {
    userService.forgotPassword(token, changePassword);
    return ResponseEntity.ok(ApiResponse.success("Password Updated Successfully", null));
  }

  /**
   * Deprecated Legacy Image Upload (Use /update instead).
   *
   * @param file the image file to be saved
   * @param authentication the authentication object containing the user's principal
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} indicating success
   * @throws IOException if an error occurs during image saving
   */
  @PostMapping("/saveImage")
  public ResponseEntity<ApiResponse<String>> saveImage(
      @RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
    userService.saveImage(file, authentication);
    return ResponseEntity.ok(ApiResponse.success(SUCCESS, null));
  }

  /**
   * Get privacy settings for the authenticated user.
   *
   * @param authentication the authentication object containing the user's principal
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} with the privacy settings
   */
  @GetMapping("/privacy")
  public ResponseEntity<ApiResponse<PrivacySettingsDTO>> getPrivacySettings(
      Authentication authentication) {
    UUID userId = UUID.fromString(authentication.getName());
    TUserDTO user = userService.findUserByKeyCloakId(userId);
    PrivacySettingsDTO result = userService.getPrivacySettings(user.getId());
    return ResponseEntity.ok(ApiResponse.success(SUCCESS, result));
  }

  /**
   * Update privacy settings for the authenticated user.
   *
   * @param authentication the authentication object containing the user's principal
   * @param dto the object containing updated privacy settings
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} indicating success
   */
  @PutMapping("/privacy")
  public ResponseEntity<ApiResponse<Void>> updatePrivacySettings(
      Authentication authentication, @RequestBody PrivacySettingsDTO dto) {
    UUID userId = UUID.fromString(authentication.getName());
    TUserDTO user = userService.findUserByKeyCloakId(userId);
    userService.updatePrivacySettings(user.getId(), dto);
    return ResponseEntity.ok(ApiResponse.success("Privacy settings updated", null));
  }

  /**
   * Block a user.
   *
   * @param authentication the authentication object containing the user's principal
   * @param blockedId the unique identifier of the user to be blocked
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} indicating success
   */
  @PostMapping("/block/{blockedId}")
  public ResponseEntity<ApiResponse<Void>> blockUser(
      Authentication authentication, @PathVariable UUID blockedId) {
    UUID userId = UUID.fromString(authentication.getName());
    TUserDTO user = userService.findUserByKeyCloakId(userId);
    userService.blockUser(user.getId(), blockedId);
    return ResponseEntity.ok(ApiResponse.success("User blocked", null));
  }

  /**
   * Unblock a user.
   *
   * @param authentication the authentication object containing the user's principal
   * @param blockedId the unique identifier of the user to be unblocked
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} indicating success
   */
  @DeleteMapping("/unblock/{blockedId}")
  public ResponseEntity<ApiResponse<Void>> unblockUser(
      Authentication authentication, @PathVariable UUID blockedId) {
    UUID userId = UUID.fromString(authentication.getName());
    TUserDTO user = userService.findUserByKeyCloakId(userId);
    userService.unblockUser(user.getId(), blockedId);
    return ResponseEntity.ok(ApiResponse.success("User unblocked", null));
  }

  /**
   * Get the list of blocked users for the authenticated user.
   *
   * @param authentication the authentication object containing the user's principal
   * @return a {@link ResponseEntity} containing an {@link ApiResponse} with the list of blocked
   *     users
   */
  @GetMapping("/blocked")
  public ResponseEntity<ApiResponse<List<TUserDTO>>> getBlockedUsers(
      Authentication authentication) {
    UUID userId = UUID.fromString(authentication.getName());
    TUserDTO user = userService.findUserByKeyCloakId(userId);
    List<TUserDTO> result = userService.getBlockedUsers(user.getId());
    return ResponseEntity.ok(ApiResponse.success(SUCCESS, result));
  }

  /** Triggers a manual re-sync of all active users to the search service. */
  @PostMapping("/sync-all-to-search")
  public ResponseEntity<ApiResponse<Void>> syncAllToSearch() {
    log.info("Manual re-sync triggered via endpoint");
    userService.syncAllUsersToSearch();
    return ResponseEntity.ok(ApiResponse.success("Re-sync initiated successfully", null));
  }
}
