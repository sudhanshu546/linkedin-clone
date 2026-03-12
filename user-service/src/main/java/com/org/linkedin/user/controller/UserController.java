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
import java.util.Optional;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The type User controller. */
@RestController
@RequestMapping("${apiPrefix}/user")
@Slf4j
@AllArgsConstructor
@Validated
public class UserController {

  private final UserService userService;
  private final KeycloakClients keycloakClient;

  /**
   * Creates a new user in the system.
   *
   * <p>This method handles the creation of a new user by taking an {@link userDTO} object which
   * contains the user details. It checks if the password and confirm password fields match. If they
   * do not match, it throws a {@link CommonExceptionHandler} with an appropriate error message. If
   * the passwords match, it proceeds to save the user using the {@link UserService}.
   *
   * @param userDTO the user DTO object containing the user details, must not be null and should be
   *     valid
   * @return a {@link ResponseEntity} containing a {@link BaseResponse} object with the status and
   *     message indicating the result of the create operation
   * @throws CommonExceptionHandler if the password and confirm password do not match, with a status
   *     code of {@link HttpStatus#BAD_REQUEST}
   */
  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant')")
  @PostMapping("/add")
  public ResponseEntity<BaseResponse<Void>> createUser(@Valid @RequestBody TUserDTO userDTO) {
    log.trace("Enter createUser method :: [{}]", userDTO);
    userService.save(userDTO, keycloakClient.clientName());
    BaseResponse<Void> returnValue =
        BaseResponse.<Void>builder()
            .status(HttpStatus.CREATED.value())
            .message("User created successfully.")
            .build();
    log.trace("Exit createUser method :: [{}]", returnValue);
    return ResponseEntity.ok(returnValue);
  }

  /**
   * Updates the user details by their ID.
   *
   * <p>This method allows the authenticated user to update their profile information, including
   * uploading a profile image and updating other user details provided in the {@link TUserDTO}
   * object. The user's ID is extracted from the {@link Authentication} object, and the update is
   * performed using the {@link UserService#updateUserById} method.
   *
   * <p>The user's ID is retrieved from this object.
   *
   * @param image the profile image to be updated. This is an optional parameter.
   * @param userDTO the {@link TUserDTO} object containing updated user details.
   * @return a {@link ResponseEntity} containing a {@link BaseResponse} with a success status and a
   *     message indicating that the user has been updated successfully.
   * @throws IOException if there is an error processing the image upload.
   */
  @Operation(summary = "This api used to update user details with profile image")
  @PutMapping("/updatebyId")
  public ResponseEntity<BaseResponse<String>> updateUserById(
      @RequestParam(value = "img", required = false) MultipartFile image,
      @ModelAttribute TUserDTO userDTO)
      throws IOException {
    log.trace("Enter updateUserById method:: userDTO [{}]", userDTO);
    userService.updateUserById(userDTO.getId(), image, userDTO);
    BaseResponse<String> returnValue =
        BaseResponse.<String>builder()
            .status(HttpStatus.OK.value())
            .message("User Updated successfully")
            .build();
    log.trace("Exit updateUserById method :: [{}]", returnValue);
    return ResponseEntity.ok(returnValue);
  }

  /**
   * Updates the user details by their ID.
   *
   * <p>This method allows the authenticated user to update their profile information, including
   * uploading a profile image and updating other user details provided in the {@link TUserDTO}
   * object. The user's ID is extracted from the {@link Authentication} object, and the update is
   * performed using the {@link UserService#updateUserById} method.
   *
   * <p>The user's ID is retrieved from this object.
   *
   * @param image the profile image to be updated. This is an optional parameter.
   * @param userDTO the {@link TUserDTO} object containing updated user details.
   * @return a {@link ResponseEntity} containing a {@link BaseResponse} with a success status and a
   *     message indicating that the user has been updated successfully.
   * @throws IOException if there is an error processing the image upload.
   */
  @Operation(summary = "This api used to update user details")
  @PutMapping("/update")
  public ResponseEntity<BaseResponse<String>> updateUser(
      @RequestParam(value = "img", required = false) MultipartFile image,
      @RequestBody TUserDTO userDTO)
      throws IOException {
    log.trace("Enter updateUser method:: userDTO [{}]", userDTO);
    userService.updateUserById(userDTO.getId(), image, userDTO);
    BaseResponse<String> returnValue =
        BaseResponse.<String>builder()
            .status(HttpStatus.OK.value())
            .message("User Updated successfully")
            .build();
    log.trace("Exit updateUser method :: [{}]", returnValue);
    return ResponseEntity.ok(returnValue);
  }

  /**
   * Retrieves the details of the currently authenticated user.
   *
   * <p>This method fetches the user details based on the UUID extracted from the authentication
   * object. It assumes that the authentication name is the UUID of the user.
   *
   * @param authentication The security context of the authenticated user, which should contain the
   *     user's UUID as the name.
   * @return A {@link ResponseEntity} containing a {@link BaseResponse} with the user details. The
   *     status is set to {@code HttpStatus.OK} if the operation is successful.
   */
  //  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant') or hasRole('User')")
  @GetMapping("/detail")
  public ResponseEntity<BaseResponse<TUserDTO>> getUserDetails(Authentication authentication) {
    log.trace("Enter getUserDetails method.");
    TUserDTO userDetails = userService.getUserDetailsByAuthentication(authentication);
    log.info("User Details :: [{}]", userDetails);
    BaseResponse<TUserDTO> returnValue =
        BaseResponse.<TUserDTO>builder()
            .status(HttpStatus.OK.value())
            .message("Operation Completed Successfully")
            .result(userDetails)
            .build();
    log.trace("Exit getUserDetails method :: [{}]", returnValue);
    return ResponseEntity.ok(returnValue);
  }

  /**
   * Retrieves an user by their unique identifier.
   *
   * <p>This method fetches the user details from the service layer using the provided UUID. If the
   * user exists, their data is returned wrapped in a {@link ResponseEntity} with an HTTP status of
   * OK. If the user does not exist, a {@link ResponseEntity} with an HTTP status of NOT_FOUND is
   * returned.
   *
   * @param id the UUID of the user to retrieve
   * @return a {@link ResponseEntity} containing a {@link BaseResponse} with the user details or a
   *     not found status
   */
  @PreAuthorize("hasRole('SystemAdmin') Or hasRole('Tenant') Or hasRole('User')")
  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<TUserDTO>> getUser(@PathVariable UUID id) {
    log.trace("Enter getUser method :: id [{}]", id);
    Optional<TUserDTO> userDTO = userService.findOne(id);
    log.trace("Exit getUser method :: [{}]", userDTO);
    BaseResponse<TUserDTO> returnValue =
        BaseResponse.<TUserDTO>builder()
            .status(HttpStatus.OK.value())
            .result(userDTO.get())
            .build();
    log.trace("Exit getUser method :: [{}]", returnValue);
    return ResponseEntity.ok(returnValue);
  }

  /**
   * Retrieves the details of an user by their unique identifier.
   *
   * <p>This method fetches the user details from the service layer using the provided UUID. If the
   * user exists, their data is returned wrapped in a {@link ResponseEntity} with an HTTP status of
   * OK.
   *
   * @param id the UUID of the user to retrieve
   * @return a {@link ResponseEntity} containing a {@link BaseResponse} with the user details
   */
  //  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant')")
  @GetMapping("/detail/{id}")
  public ResponseEntity<BaseResponse<TUserDTO>> getUsersDetailsById(@PathVariable UUID id) {
    log.trace("Enter getUsersDetailsById id :: [{}]", id);
    TUserDTO userDetails = userService.getUserDetails(id);
    log.info("User Detail :: [{}]", userDetails);
    BaseResponse<TUserDTO> returnValue =
        BaseResponse.<TUserDTO>builder().status(HttpStatus.OK.value()).result(userDetails).build();
    log.trace("Exit getUsersDetailsById id :: [{}]", returnValue);
    return ResponseEntity.ok(returnValue);
  }

  /**
   * Deletes an user by their unique identifier.
   *
   * <p>This method removes the user associated with the provided UUID from the system. It uses the
   * 'DEMO_CLIENT' client name for the operation context.
   *
   * @param id the UUID of the user to delete
   * @return a {@link ResponseEntity} containing a {@link BaseResponse} indicating the outcome of
   *     the operation
   */
  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant')")
  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<Void>> delete(@PathVariable UUID id) {
    log.trace("Enter delete method :: id [{}]", id);
    userService.delete(keycloakClient.clientName(), id);
    BaseResponse<Void> returnValue =
        BaseResponse.<Void>builder()
            .status(HttpStatus.OK.value())
            .message("Data deleted successfully")
            .build();
    log.trace("Exit delete method :: [{}]", returnValue);
    return ResponseEntity.ok(returnValue);
  }

  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant') or hasRole('User')")
  @GetMapping("/search")
  public ResponseEntity<BaseResponse<List<TUserDTO>>> searchUsers(@RequestParam String query) {
    log.trace("Enter searchUsers method :: query [{}]", query);
    List<TUserDTO> users = userService.searchUsers(query);
    BaseResponse<List<TUserDTO>> response =
        BaseResponse.<List<TUserDTO>>builder()
            .status(HttpStatus.OK.value())
            .message(SUCCESS)
            .result(users)
            .build();
    log.trace("Exit searchUsers method :: [{}]", response);
    return ResponseEntity.ok(response);
  }

  /**
   * Retrieves a paginated list of all users.
   *
   * <p>This method fetches a page of {@link "UserDTO"} objects based on the provided {@link
   * Pageable} object. It uses the {@link UserService} to retrieve the data, which is then wrapped
   * in a {@link BasePageResponse} to include additional pagination information such as page number,
   * page size, and total number of records.
   *
   * @param pageable The pagination information and sorting criteria.
   * @return A {@link ResponseEntity} containing a {@link BasePageResponse} with the list of {@link
   *     "UserDTO"}, along with pagination details and a status message.
   */
  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant')")
  @GetMapping("/getAllUserDetail")
  public ResponseEntity<BaseResponse<List<TUserDTO>>> getAllUserDetail(Pageable pageable) {
    log.trace("Enter getAllUserDetail method.");
    Page<TUserDTO> userDetails = userService.getAllUserDetail(pageable);
    log.info("User Detail :: [{}]", userDetails);
    BasePageResponse<List<TUserDTO>> returnValue =
        BasePageResponse.<List<TUserDTO>>builder()
            .status(HttpStatus.OK.value())
            .message("Operation Completed Successfully")
            .result(userDetails.getContent())
            .pageNumber(userDetails.getNumber())
            .pageSize(userDetails.getSize())
            .totalRecords(userDetails.getTotalElements())
            .build();
    log.trace("Exit getAllUserDetail method :: [{}]", returnValue);
    return ResponseEntity.ok(returnValue);
  }

  /**
   * Handles the password reset functionality for an authenticated user.
   *
   * <p>This method validates the {@link ChangePassword} object using the provided {@link
   * BindingResult}. If there are validation errors, it throws a {@link CommonExceptionHandler} with
   * the error details. Otherwise, it retrieves the client name from the JWT claims in the {@link
   * Authentication} object, and calls the {@link UserService#resetPassword} method to update the
   * password.
   *
   * @param authentication The security context of the authenticated user, containing the JWT.
   * @param changePassword The DTO containing the new password details.
   * @param bindingResult The result of the validation of the {@link ChangePassword} object.
   * @return A {@link ResponseEntity} containing a {@link BaseResponse} with the status and message
   *     of the operation.
   * @throws CommonExceptionHandler If there are validation errors in the {@link ChangePassword}
   *     object.
   */
  @PreAuthorize("hasRole('SystemAdmin') or hasRole('Tenant')")
  @PutMapping("/reset-password")
  public ResponseEntity<BaseResponse<String>> resetPassword(
      Authentication authentication,
      @RequestBody @Valid ChangePassword changePassword,
      BindingResult bindingResult) {
    log.trace(
        "Enter resetPassword method :: changePassword [{}] :: bindingResult [{}]",
        changePassword,
        bindingResult);
    if (bindingResult.hasErrors()) {
      String errors =
          bindingResult.getAllErrors().stream()
              .map(ObjectError::getDefaultMessage)
              .collect(Collectors.joining(", "));
      throw new CommonExceptionHandler(errors, HttpStatus.BAD_REQUEST.value());
    }
    String clientName = (((Jwt) authentication.getPrincipal()).getClaims()).get("azp").toString();
    log.info("Client name is :: [{}]", clientName);
    userService.resetPassword(changePassword, clientName);
    BaseResponse<String> returnValue =
        BaseResponse.<String>builder()
            .status(HttpStatus.OK.value())
            .message("Password Updated successfully")
            .build();
    log.trace("Exit resetPassword method :: [{}]", returnValue);
    return ResponseEntity.ok(returnValue);
  }

  /**
   * API endpoint to send a password reset link to the provided user's email. This endpoint is
   * invoked via a GET request, and it triggers the generation of a secure reset link and sends it
   * to the specified email asynchronously.
   *
   * @param email The email address of the user requesting the password reset.
   * @return A ResponseEntity containing a BaseResponse with the status, message, and the generated
   *     reset link.
   * @throws Exception If the user is not found or an error occurs during the process of generating
   *     and sending the reset link.
   */
  @Operation(summary = "This Api is use for to send forgot password link on mail")
  @GetMapping("/getForgotLink")
  public ResponseEntity<BaseResponse<String>> sendForgotLink(@RequestParam String email)
      throws Exception {
    log.trace("Enter sendForgotLink method.");
    String forgotlink = userService.sendForgotLink(email);
    BaseResponse<String> returnValue =
        BaseResponse.<String>builder()
            .status(HttpStatus.OK.value())
            .message("Link Sent Successfully on Mail")
            .result(forgotlink)
            .build();
    log.trace("Exit sendForgotLink method :: [{}]", returnValue);
    return ResponseEntity.ok(returnValue);
  }

  /**
   * API endpoint to handle the password reset process for a user using a token. This endpoint is
   * invoked via a GET request and processes a password reset by validating the provided token and
   * updating the password if the token is valid.
   *
   * @param token The token received by the user in the reset password link.
   * @param changePassword The request body containing the new password details.
   * @return A ResponseEntity containing a BaseResponse with the status and message.
   * @throws Exception If the token is invalid, expired, or any error occurs during the password
   *     reset process.
   */
  @Operation(summary = "This Api is use to change password")
  @PostMapping("/forgotPassword")
  public ResponseEntity<BaseResponse<String>> forgotPassword(
      @RequestParam String token, @RequestBody ChangePassword changePassword) throws Exception {
    log.trace("Enter forgotPassword method.");
    userService.forgotPassword(token, changePassword);
    BaseResponse<String> returnValue =
        BaseResponse.<String>builder()
            .status(HttpStatus.OK.value())
            .message("Password Updated Successfully")
            .build();
    log.trace("Exit forgotPassword method :: [{}]", returnValue);
    return ResponseEntity.ok(returnValue);
  }

  /**
   * Uploads an image file for the authenticated user.
   *
   * @param file the image file to upload
   * @param authentication the authentication object containing user details
   * @return a ResponseEntity containing the response status and message
   * @throws IOException if an error occurs while saving the image
   */
  @Operation(summary = "This api is used to upload profile image")
  @PostMapping("/upload")
  public ResponseEntity<BaseResponse<String>> uploadImage(
      @RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
    userService.saveImage(file, authentication);
    BaseResponse<String> response =
        BaseResponse.<String>builder().status(HttpStatus.OK.value()).message(SUCCESS).build();
    return ResponseEntity.ok(response);
  }

  /**
   * Retrieves the image for the authenticated user.
   *
   * @param authentication the authentication object containing user details
   * @return a ResponseEntity containing the image data in bytes
   */
  @Operation(summary = "This api used for get user's profile image")
  @GetMapping("/image")
  public ResponseEntity<BaseResponse<byte[]>> getImage(Authentication authentication) {
    byte[] image = userService.getImage(authentication);
    BaseResponse<byte[]> response =
        BaseResponse.<byte[]>builder()
            .status(HttpStatus.OK.value())
            .message(SUCCESS)
            .result(image)
            .build();
    return ResponseEntity.ok(response);
  }

  /**
   * Retrieves a user by their Keycloak ID.
   *
   * @param keyCloakId the user's Keycloak UUID
   * @return ResponseEntity with user details in a BaseResponse and HTTP status 200 (OK)
   */
  @GetMapping("/user/{id}")
  public ResponseEntity<BaseResponse<TUserDTO>> getUserByKeyCloakId(
      @PathVariable("id") UUID keyCloakId) {
    TUserDTO userDTO = userService.findUserByKeyCloakId(keyCloakId);
    BaseResponse<TUserDTO> response =
        BaseResponse.<TUserDTO>builder()
            .status(HttpStatus.OK.value())
            .message(SUCCESS)
            .result(userDTO)
            .build();
    return ResponseEntity.ok(response);
  }
}
