package com.org.linkedin.user.controller;

import static com.org.linkedin.utility.CommonConstants.SUCCESS;

import com.org.linkedin.dto.BasePageResponse;
import com.org.linkedin.dto.BaseResponse;
import com.org.linkedin.dto.user.ChangePassword;
import com.org.linkedin.dto.user.TUserDTO;
import com.org.linkedin.user.config.keycloak.KeycloakClients;
import com.org.linkedin.user.service.UserService;
import com.org.linkedin.utility.exception.CommonExceptionHandler;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Consolidated User Controller
 */
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
   */
  @PostMapping("/add")
  public ResponseEntity<BaseResponse<Void>> createUser(@Valid @RequestBody TUserDTO userDTO) {
    log.trace("Enter createUser method :: [{}]", userDTO);
    userService.save(userDTO, keycloakClient.clientName());
    return ResponseEntity.ok(BaseResponse.<Void>builder()
            .status(HttpStatus.CREATED.value())
            .message("User created successfully.")
            .build());
  }

  /**
   * Update user details with optional profile image.
   */
  @Operation(summary = "Update user profile details and/or image")
  @PutMapping("/update")
  public ResponseEntity<BaseResponse<String>> updateUser(
      @RequestParam(value = "img", required = false) MultipartFile image,
      @ModelAttribute TUserDTO userDTO)
      throws IOException {
    log.trace("Enter updateUser method:: userDTO [{}]", userDTO);
    userService.updateUserById(userDTO.getId(), image, userDTO);
    return ResponseEntity.ok(BaseResponse.<String>builder()
            .status(HttpStatus.OK.value())
            .message("User Updated successfully")
            .build());
  }

  /**
   * Get details of the currently authenticated user.
   */
  @GetMapping("/me")
  public ResponseEntity<BaseResponse<TUserDTO>> getAuthenticatedUser(Authentication authentication) {
    log.trace("Enter getAuthenticatedUser method.");
    TUserDTO userDetails = userService.getUserDetailsByAuthentication(authentication);
    return ResponseEntity.ok(BaseResponse.<TUserDTO>builder()
            .status(HttpStatus.OK.value())
            .message(SUCCESS)
            .result(userDetails)
            .build());
  }

  /**
   * Get any user by their ID (handles both internal UUID and Keycloak UUID via service logic).
   * Alias /user/{id} added for backward compatibility with Feign clients.
   */
  @GetMapping({"/{id}", "/user/{id}"})
  public ResponseEntity<BaseResponse<TUserDTO>> getUser(@PathVariable UUID id) {
    log.trace("Enter getUser method :: id [{}]", id);
    TUserDTO userDTO = userService.findOne(id)
        .orElseGet(() -> userService.findUserByKeyCloakId(id));
    
    return ResponseEntity.ok(BaseResponse.<TUserDTO>builder()
            .status(HttpStatus.OK.value())
            .result(userDTO)
            .build());
  }

  /**
   * Delete a user.
   */
  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant')")
  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<Void>> delete(@PathVariable UUID id) {
    log.trace("Enter delete method :: id [{}]", id);
    userService.delete(keycloakClient.clientName(), id);
    return ResponseEntity.ok(BaseResponse.<Void>builder()
            .status(HttpStatus.OK.value())
            .message("Data deleted successfully")
            .build());
  }

  /**
   * Search users by name or email.
   */
  @GetMapping("/search")
  public ResponseEntity<BaseResponse<List<TUserDTO>>> searchUsers(@RequestParam String query) {
    List<TUserDTO> users = userService.searchUsers(query);
    return ResponseEntity.ok(BaseResponse.<List<TUserDTO>>builder()
            .status(HttpStatus.OK.value())
            .message(SUCCESS)
            .result(users)
            .build());
  }

  /**
   * Paginated list of all users.
   */
  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant')")
  @GetMapping("/getAllUserDetail")
  public ResponseEntity<BaseResponse<List<TUserDTO>>> getAllUserDetail(Pageable pageable) {
    Page<TUserDTO> userDetails = userService.getAllUserDetail(pageable);
    return ResponseEntity.ok(BasePageResponse.<List<TUserDTO>>builder()
            .status(HttpStatus.OK.value())
            .message(SUCCESS)
            .result(userDetails.getContent())
            .pageNumber(userDetails.getNumber())
            .pageSize(userDetails.getSize())
            .totalRecords(userDetails.getTotalElements())
            .build());
  }

  /**
   * Reset password for authenticated user.
   */
  @PutMapping("/reset-password")
  public ResponseEntity<BaseResponse<String>> resetPassword(
      Authentication authentication,
      @RequestBody @Valid ChangePassword changePassword,
      BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      String errors = bindingResult.getAllErrors().stream()
              .map(ObjectError::getDefaultMessage)
              .collect(Collectors.joining(", "));
      throw new CommonExceptionHandler(errors, HttpStatus.BAD_REQUEST.value());
    }
    String clientName = (((Jwt) authentication.getPrincipal()).getClaims()).get("azp").toString();
    userService.resetPassword(changePassword, clientName);
    return ResponseEntity.ok(BaseResponse.<String>builder()
            .status(HttpStatus.OK.value())
            .message("Password Updated successfully")
            .build());
  }

  /**
   * Request forgot password link.
   */
  @GetMapping("/getForgotLink")
  public ResponseEntity<BaseResponse<String>> sendForgotLink(@RequestParam String email) throws Exception {
    String forgotlink = userService.sendForgotLink(email);
    return ResponseEntity.ok(BaseResponse.<String>builder()
            .status(HttpStatus.OK.value())
            .message("Link Sent Successfully on Mail")
            .result(forgotlink)
            .build());
  }

  /**
   * Process forgot password token.
   */
  @PostMapping("/forgotPassword")
  public ResponseEntity<BaseResponse<String>> forgotPassword(
      @RequestParam String token, @RequestBody ChangePassword changePassword) throws Exception {
    userService.forgotPassword(token, changePassword);
    return ResponseEntity.ok(BaseResponse.<String>builder()
            .status(HttpStatus.OK.value())
            .message("Password Updated Successfully")
            .build());
  }

  /**
   * Deprecated Legacy Image Upload (Use /update instead).
   */
  @PostMapping("/saveImage")
  public ResponseEntity<BaseResponse<String>> saveImage(
      @RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
    userService.saveImage(file, authentication);
    return ResponseEntity.ok(BaseResponse.<String>builder().status(HttpStatus.OK.value()).message(SUCCESS).build());
  }
}
