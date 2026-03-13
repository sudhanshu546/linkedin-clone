package com.org.linkedin.user.service.impl;

import static com.org.linkedin.utility.errors.ErrorKeys.FILE_SIZE_EXCEEDED;
import static com.org.linkedin.utility.errors.ErrorKeys.INVALID_FILE_FORMAT;
import static com.org.linkedin.utility.errors.ErrorKeys.INVALID_TOKEN_FORMAT;
import static com.org.linkedin.utility.errors.ErrorKeys.USER_NOT_FOUND;

import com.org.linkedin.domain.user.Role;
import com.org.linkedin.domain.user.TUser;
import com.org.linkedin.dto.user.ChangePassword;
import com.org.linkedin.dto.user.TUserDTO;
import com.org.linkedin.user.config.AsyncCalls;
import com.org.linkedin.user.config.CryptUtil;
import com.org.linkedin.user.config.keycloak.KeycloakClients;
import com.org.linkedin.user.mapper.TUserMapper;
import com.org.linkedin.user.repository.RoleRepository;
import com.org.linkedin.user.repository.UserRepository;
import com.org.linkedin.user.service.UserService;
import com.org.linkedin.user.service.storage.FileStorageService;
import com.org.linkedin.user.utility.KeyCloakUtil;
import com.org.linkedin.utility.errors.ErrorKeys;
import com.org.linkedin.utility.exception.CommonExceptionHandler;
import com.org.linkedin.utility.service.CommonUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

  private final RoleRepository roleRepository;

  private final KeyCloakUtil keyCloakUtil;

  private final TUserMapper userMapper;

  private final KeycloakClients keycloakDemoClient;

  private final AsyncCalls asyncCalls;

  @Value("${validation.file.size}")
  private String MAX_FILE_SIZE; // 300KB

  @Value("${user.createPasswordUrl}")
  private String createPasswordUrl;

  private static final List<String> ALLOWED_FILE_TYPES =
      Arrays.asList(MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE);

  @PersistenceContext private final EntityManager entityManager;

  private final CommonUtil commonUtil;

  private final FileStorageService fileStorageService;

  @Override
  public List<TUserDTO> searchUsers(String query) {
    log.trace("Enter searchUsers method :: query [{}]", query);
    List<TUser> users =
        userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            query, query);
    log.trace("Exit searchUsers method");
    return users.stream().map(userMapper::toDto).collect(Collectors.toList());
  }

  /**
   * Saves an user to the database and Keycloak.
   *
   * <p>This method first converts the email in the {@link "UserDTO"} to lowercase and checks for
   * its existence in the database. If the email already exists, it throws a {@link
   * CommonExceptionHandler} with the error key {@link
   * ErrorKeys#USER_ALREADY_EXISTS_WITH_THIS_EMAIL}. If this combination exists, it throws a {@link
   * CommonExceptionHandler} with the error key {@link ErrorKeys#"ID_NUMBER_EXISTS_ON_THIS_NIN"}.
   *
   * <p>If the checks pass, the method proceeds to map the {@link "UserDTO"} to an {@link "User"}
   * entity, save the user in Keycloak, and then save the user entity in the database. The Keycloak
   * user ID is also set in the user entity before saving it.
   *
   * @param userDTO the user data transfer object containing user details
   * @param clientName the client name for which the user is being saved, used for Keycloak
   *     configuration
   * @throws CommonExceptionHandler if the email already exists
   */
  @Override
  public void save(TUserDTO userDTO, String clientName) {
    log.trace("Enter save method :: userDTO [{}] :: clientName [{}]", userDTO, clientName);

    if (userRepository.existsByEmail(userDTO.getEmail())) {
      log.debug("Duplicate User found to save User Request ::  [{}]", userDTO);
      throw new CommonExceptionHandler(
          ErrorKeys.USER_ALREADY_EXISTS_WITH_THIS_EMAIL, HttpStatus.BAD_REQUEST.value());
    }

    //    UUID roleId = userDTO.getRole().getId();

    //    if (roleId == null) {
    //      throw new CommonExceptionHandler(ErrorKeys.INVALID_ROLE,
    // HttpStatus.BAD_REQUEST.value());
    //    }
    //    Optional<Role> roleOpt = roleRepository.findById(roleId);
    //    if (roleOpt.isEmpty()) {
    //      throw new CommonExceptionHandler(ErrorKeys.INVALID_ROLE_ID,
    // HttpStatus.BAD_REQUEST.value());
    //    }
    //    Role dbRole = roleOpt.get();
    TUser user = userMapper.toEntity(userDTO);
    log.info("user is :: [{}]", user);
    UUID keyCloakUserId = UUID.fromString(keyCloakUtil.saveUser(userDTO, clientName, "USER"));
    String password = generatePassword();
    userDTO.setPassword(password);
    log.trace("Key Cloak user id is :: [{}]", keyCloakUserId);
    user.setKeycloakUserId(keyCloakUserId);
    user.setPassword(Base64.getEncoder().encodeToString(userDTO.getPassword().getBytes()));
    // save email in db
    user.setEmail(user.getEmail().toLowerCase());
    user = userRepository.save(user);
    userMapper.toDto(user);
    // Send Password on Mail
    //    asyncCalls.sendRegistrationMail(user.getFirstName(), user.getEmail(), password);
    log.trace("Exit save method .");
  }

  /**
   * Retrieves the details of an user based on their Keycloak ID.
   *
   * <p>This method searches for an user using their Keycloak ID. If the user is not found, it
   * throws an {@link EntityNotFoundException} with the message "user not found".
   *
   * <p>
   *
   * @param userId the UUID of the user as identified in Keycloak
   * @return an {@link "UserDTO"} containing the details of the found user
   * @throws EntityNotFoundException if no user is found with the provided Keycloak ID
   */
  @Override
  public TUserDTO getUserDetails(UUID userId) {
    log.trace("Enter getUserDetails method :: [{}]", userId);
    Optional<TUser> optionalUser = userRepository.findById(userId);
    log.info("User details :: [{}]", optionalUser);
    TUser existingUser =
        optionalUser.orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
    log.trace("Exit getUserDetails method :: [{}]", existingUser);
    return userMapper.toDto(existingUser);
  }

  /**
   * Retrieves the details of an user based on their Keycloak ID.
   *
   * <p>This method searches for an user using their Keycloak ID. If the user is not found, it
   * throws an {@link EntityNotFoundException} with the message "user not found".
   *
   * <p>
   *
   * @param userId the UUID of the user as identified in Keycloak
   * @return an {@link "UserDTO"} containing the details of the found user
   * @throws EntityNotFoundException if no user is found with the provided Keycloak ID
   */
  @Override
  public TUserDTO getUserDetailsByAuthentication(Authentication authentication) {
    log.trace("Enter getUserDetails method :: [{}]", authentication);

    // Extract authorities
    Set<String> authorities =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

    // Retrieve user details
    TUser existingUser =
        userRepository
            .findById(UUID.fromString(authentication.getName()))
            .orElseThrow(EntityNotFoundException::new);

    log.info("User details :: [{}]", existingUser);

    // Map user entity to DTO
    TUserDTO tUserDTO = userMapper.toDto(existingUser);

    log.trace("Exit getUserDetails method :: [{}]", tUserDTO);
    return tUserDTO;
  }

  /**
   * Retrieves an user by their unique identifier if they have not been marked as deleted.
   *
   * <p>This method performs a database search for an user using the provided UUID. It only returns
   * the user if they are not flagged as deleted. The result is then mapped from an {@link "User"}
   * entity to an {@link "UserDTO"} data transfer object.
   *
   * <p>
   *
   * @param id the UUID of the user to retrieve
   * @return an {@link Optional<UserDTO>} containing the user details if found, or an empty Optional
   *     if no matching user is found
   */
  @Override
  @Transactional(readOnly = true)
  public Optional<TUserDTO> findOne(UUID id) {
    log.trace("Enter findOne method :: id [{}]", id);
    Optional<TUser> user = userRepository.findByIdAndIsDeletedFalse(id);
    log.trace("Exit findOne method :: user [{}]", user);
    return user.map(userMapper::toDto);
  }

  /**
   * Deletes an user from both the application database and Keycloak.
   *
   * <p>This method first deletes the user from Keycloak using the provided client name and user ID.
   * It then marks the user as deleted in the application database by setting the `isDeleted` flag
   * to true. If the user with the given Keycloak ID does not exist in the database, it throws an
   * {@link EntityNotFoundException}.
   *
   * <p>
   *
   * @param clientName the client name for the Keycloak configuration
   * @param id the UUID of the user to be deleted
   * @throws EntityNotFoundException if no user is found with the provided Keycloak ID
   */
  @Override
  public void delete(String clientName, UUID id) {
    log.trace("Enter delete method  :: client name [{}] :: id [{}]", clientName, id);
    TUser user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND + ": " + id));
    String keyCloakId = String.valueOf(user.getKeycloakUserId());
    Response response = keyCloakUtil.deleteUserFromKeycloak(clientName, keyCloakId);
    log.info("delete user from key cloak :: [{}]", response);
    user.setIsDeleted(true);
    log.trace("Exit delete method :: [{}]", user);
    userRepository.save(user);
  }

  /**
   * Retrieves a paginated list of all users who have not been marked as deleted.
   *
   * <p>This method utilizes the {@link Pageable} object to fetch a page of users from the database.
   * It filters out any users that have been marked as deleted. The retrieved {@link Page} of {@link
   * "User"} entities is then mapped to a {@link Page} of {@link "UserDTO"} using the {@link
   * "UserMapper"}.
   *
   * <p>
   *
   * @param pageable the pagination information
   * @return a {@link Page} of {@link "UserDTO"} containing user details
   */
  @Override
  public Page<TUserDTO> getAllUserDetail(Pageable pageable) {
    log.trace("Enter getAllUserDetail method .");
    Page<TUser> userDetails = userRepository.findAllByIsDeletedFalse(pageable);
    log.info("User details is :: [{}]", userDetails);
    log.trace("Exit getAllUserDetail method.");
    return userDetails.map(userMapper::toDto);
  }

  /**
   * Resets the password of an user in both the application database and Keycloak.
   *
   * <p>This method first checks if the new password and confirm password match. If they do not
   * match, it throws a {@link CommonExceptionHandler} with the error key {@link
   * ErrorKeys#PASSWORD_AND_CONFIRM_PASSWORD_DO_NOT_MATCH}. It then retrieves the user by their ID.
   * If the user is not found, it throws a {@link CommonExceptionHandler} with the error key {@link
   * ErrorKeys#USER_NOT_FOUND}. If the user is found, it updates the user's password in Keycloak
   * using the Keycloak ID associated with the user.
   *
   * @param changePasswordDTO the DTO containing the user ID, new password, and confirm password
   * @param clientName the client name for the Keycloak configuration
   * @throws CommonExceptionHandler if the passwords do not match or if no user is found with the
   *     provided ID
   */
  @Override
  public void resetPassword(ChangePassword changePasswordDTO, String clientName) {
    log.trace(
        "Enter resetPassword method ::  changePasswordDTO [{}] :: clientName [{}]",
        changePasswordDTO);
    String newPassword = changePasswordDTO.getNewPassword();
    String confirmPassword = changePasswordDTO.getConfirmPassword();
    log.info("newPassword :: [{}]", newPassword);
    log.info("confirmPassword :: [{}]", confirmPassword);
    if (!newPassword.equals(confirmPassword)) {
      log.debug(
          "Password and ConfirmConfirm Password Doesn't match in ChangedPassword Request :: Password [{}] ,ConfirmPassword [{}] ",
          changePasswordDTO.getNewPassword(),
          changePasswordDTO.getConfirmPassword());
      throw new CommonExceptionHandler(
          ErrorKeys.PASSWORD_AND_CONFIRM_PASSWORD_DO_NOT_MATCH, HttpStatus.BAD_REQUEST.value());
    }
    if (changePasswordDTO.getEmailId().isEmpty()) {
      throw new CommonExceptionHandler("Email cannot be null", HttpStatus.BAD_REQUEST.value());
    }
    TUser user = userRepository.findByEmailAndIsDeletedFalse(changePasswordDTO.getEmailId());
    log.info("Users details using id :: [{}]", user);
    if (user == null) {
      log.debug(
          "User Not Exist with mail: " + changePasswordDTO.getEmailId(),
          changePasswordDTO.getEmpId());
      throw new CommonExceptionHandler(USER_NOT_FOUND, HttpStatus.BAD_REQUEST.value());
    }
    String keycloakId = user.getKeycloakUserId().toString();
    log.info("keycloakId :: [{}]", keycloakId);
    keyCloakUtil.updateUserPassword(keycloakId, newPassword, clientName);
    user.setPassword(Base64.getEncoder().encodeToString(newPassword.getBytes()));
    userRepository.save(user);
    log.trace("Exit resetPassword method .");
  }

  /**
   * Updates an user's details by their unique identifier.
   *
   * <p>This method retrieves an existing user by their unique identifier (usrId). If the user is
   * not found, it throws a {@link CommonExceptionHandler} with an error key {@link
   * ErrorKeys#USER_NOT_FOUND}. It checks if the identification type and ID number combination from
   * the provided {@link "UserDTO"} already exists in the database for another user. If it does, it
   * throws a {@link CommonExceptionHandler} with an error key {@link
   * ErrorKeys#"ID_NUMBER_EXISTS_ON_THIS_NIN"}.
   *
   * <p>If the user exists and the identification details are unique, it updates the user's details
   * with the new values from the {@link "UserDTO"}. It also updates the role of the user by
   * converting the {@link "RoleDTO"} to a {@link Role} entity. Finally, it saves the updated user
   * back to the database.
   *
   * @param userId the UUID of the user to update
   * @param userDTO the data transfer object containing the updated details of the user
   * @param "clientName" the client name for the Keycloak configuration
   * @throws CommonExceptionHandler if no user is found with the provided ID or if the
   *     identification type and ID number combination already exists for another user
   */
  @Override
  public void updateUserById(UUID userId, MultipartFile image, TUserDTO userDTO)
      throws IOException {
    log.trace("Enter updateUserById method  :: userId [{}] :: userDTO [{}]", userId, userDTO);
    TUser existingUser =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> {
                  log.debug("User Id doesn't exist :: id [{}]", userId);
                  throw new CommonExceptionHandler(USER_NOT_FOUND, HttpStatus.BAD_REQUEST.value());
                });
    existingUser.setFirstName(userDTO.getFirstName());
    existingUser.setLastName(userDTO.getLastName());

    if (image != null && !image.isEmpty()) {
      String fileName = fileStorageService.storeFile(image);
      existingUser.setProfileImageUrl(fileName);
    }

    existingUser = userRepository.save(existingUser);
    TUserDTO updatedUser = userMapper.toDto(existingUser);
    keyCloakUtil.updateUserDetailsInKeycloak(updatedUser, keycloakDemoClient);
    log.trace("Exit updateUserById method .");
  }

  /**
   * Sends a password reset link to the user's email. The method checks if the user exists and, if
   * found, generates a secure token containing the user's email and the current timestamp. It then
   * creates a URL with the token and sends the link to the user's email using asynchronous calls.
   *
   * @param email The email address of the user requesting a password reset.
   * @return The generated password reset URL.
   * @throws Exception If the user is not found or an error occurs during token encryption or email
   *     sending.
   */
  @Override
  public String sendForgotLink(String email) throws Exception {
    TUser tUser = userRepository.findByEmailAndIsDeletedFalse(email);
    if (tUser == null) {
      throw new CommonExceptionHandler(USER_NOT_FOUND, HttpStatus.BAD_REQUEST.value());
    }
    String token = CryptUtil.encrypt(email + "~" + System.currentTimeMillis());
    String url = createPasswordUrl + token;
    // Send Forgot Password link on Mail
    asyncCalls.sendForgotLink(tUser.getFirstName(), tUser.getEmail(), url);
    return url;
  }

  /**
   * Handles the password reset process for a user based on a provided token. The token is decrypted
   * to extract the email and timestamp. The method then checks if the token is still valid (i.e.,
   * within 24 hours of generation). If valid, it proceeds to reset the user's password.
   *
   * @param token The encrypted token that contains the user's email and timestamp in the format
   *     "email~timestamp".
   * @param changePassword The object containing the new password details to be updated for the
   *     user.
   * @throws Exception If the token is invalid, expired (older than 24 hours), or the decryption
   *     process fails.
   */
  @Override
  public void forgotPassword(String token, ChangePassword changePassword) throws Exception {
    String decryptToken = CryptUtil.decrypt(token);
    String[] parts = decryptToken.split("~");
    if (parts.length != 2) {
      throw new CommonExceptionHandler(INVALID_TOKEN_FORMAT, HttpStatus.BAD_REQUEST.value());
    }
    String email = parts[0];
    long tokenTimestamp = Long.parseLong(parts[1]);
    long currentTime = System.currentTimeMillis();
    long twentyFourHoursInMillis = 24 * 60 * 60 * 1000;
    if ((currentTime - tokenTimestamp) > twentyFourHoursInMillis) {
      throw new Exception(
          "Token has expired. The session was valid only for 24 hours, Forgot your password Again!");
    }
    changePassword.setEmailId(email);
    // Change Password in Db and Keycloak
    resetPassword(changePassword, keycloakDemoClient.clientName());
  }

  String generatePassword() {
    String allChars =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_+=<>?";
    StringBuilder password = new StringBuilder();

    Random random = new Random();
    for (int i = 0; i < 8; i++) {
      password.append(allChars.charAt(random.nextInt(allChars.length())));
    }
    return password.toString();
  }

  /**
   * Saves the uploaded image for the authenticated user.
   *
   * @param file the image file to be saved
   * @param authentication the authentication object containing user details
   * @throws IOException if an error occurs while reading the image file
   * @throws CommonExceptionHandler if the file size exceeds the maximum limit or the file format is
   *     invalid
   */
  @Override
  public void saveImage(MultipartFile file, Authentication authentication) throws IOException {

    String contentType = file.getContentType();
    long File_Size = Long.parseLong(MAX_FILE_SIZE);
    if (file.getSize() > File_Size) {
      throw new CommonExceptionHandler(FILE_SIZE_EXCEEDED, HttpStatus.BAD_REQUEST.value());
    } else if (contentType == null || !ALLOWED_FILE_TYPES.contains(contentType)) {
      throw new CommonExceptionHandler(INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST.value());
    }

    TUser user =
        userRepository
            .findById(UUID.fromString(authentication.getName()))
            .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

    String fileName = fileStorageService.storeFile(file);
    user.setProfileImageUrl(fileName);
    userRepository.save(user);
  }

  /**
   * Retrieves the image for the authenticated user.
   *
   * @param authentication the authentication object containing user details
   * @return the image data as a byte array
   * @throws CommonExceptionHandler if the image is not found for the user
   */
  @Override
  public byte[] getImage(Authentication authentication) {
    UUID userId = UUID.fromString(authentication.getName());
    TUser user = userRepository.findById(userId).get();
    //    if (user.getImage() == null) {
    //      throw new CommonExceptionHandler(FILE_NOT_FOUND, HttpStatus.BAD_REQUEST.value());
    //    }
    return null;
  }

  /**
   * Finds and returns a user by their Keycloak ID.
   *
   * @param keyCloakId the UUID of the user in Keycloak
   * @return TUserDTO object representing the user's details
   */
  @Override
  public TUserDTO findUserByKeyCloakId(UUID keyCloakId) {
    log.info("Entering findUserByKeyCloakId with keyCloakId: {}", keyCloakId);
    TUser user = userRepository.findByKeycloakUserId(keyCloakId).get();
    TUserDTO userDTO = userMapper.toDto(user);
    log.info("Exiting findUserByKeyCloakId with result: {}", userDTO);
    return userDTO;
  }
}
