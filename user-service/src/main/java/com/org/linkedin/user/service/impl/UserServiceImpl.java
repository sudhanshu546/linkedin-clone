package com.org.linkedin.user.service.impl;

import static com.org.linkedin.utility.ProjectConstants.*;
import static com.org.linkedin.utility.errors.ErrorKeys.FILE_SIZE_EXCEEDED;
import static com.org.linkedin.utility.errors.ErrorKeys.INVALID_FILE_FORMAT;
import static com.org.linkedin.utility.errors.ErrorKeys.INVALID_TOKEN_FORMAT;

import com.org.linkedin.domain.user.TUser;
import com.org.linkedin.dto.event.UserDeletedEvent;
import com.org.linkedin.dto.event.UserUpdatedEvent;
import com.org.linkedin.dto.user.ChangePassword;
import com.org.linkedin.dto.user.PrivacySettingsDTO;
import com.org.linkedin.dto.user.TUserDTO;
import com.org.linkedin.user.config.AsyncCalls;
import com.org.linkedin.user.config.CryptUtil;
import com.org.linkedin.user.config.keycloak.KeycloakClients;
import com.org.linkedin.user.domain.PrivacySettings;
import com.org.linkedin.user.domain.UserBlock;
import com.org.linkedin.user.mapper.TUserMapper;
import com.org.linkedin.user.repository.*;
import com.org.linkedin.user.service.UserService;
import com.org.linkedin.user.utility.KeyCloakUtil;
import com.org.linkedin.utility.errors.ErrorKeys;
import com.org.linkedin.utility.exception.ConflictException;
import com.org.linkedin.utility.exception.ResourceNotFoundException;
import com.org.linkedin.utility.exception.ValidationException;
import com.org.linkedin.utility.service.AdvanceSearchCriteria;
import com.org.linkedin.utility.service.CommonUtil;
import com.org.linkedin.utility.storage.FileStorageService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementation of UserService responsible for user account lifecycle, identity synchronization
 * with Keycloak, and user discovery. Uses Redis caching to provide high performance for identity
 * lookups.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

  private final RoleRepository roleRepository;

  private final PrivacySettingsRepository privacySettingsRepository;

  private final UserBlockRepository userBlockRepository;

  private final KeyCloakUtil keyCloakUtil;

  private final TUserMapper userMapper;

  private final KeycloakClients keycloakDemoClient;

  private final AsyncCalls asyncCalls;

  @Value("${validation.file.size}")
  private String MAX_FILE_SIZE; // 300KB

  private String createPasswordUrl;

  private static final List<String> ALLOWED_FILE_TYPES =
      Arrays.asList(MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE);

  @PersistenceContext private final EntityManager entityManager;

  private final CommonUtil commonUtil;

  private final FileStorageService fileStorageService;

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Value("${kafka.topics.user-deleted:user-deleted}")
  private String userDeletedTopic;

  /** Search for users by keyword (first name or last name). */
  @Override
  public List<TUserDTO> searchUsers(String query) {
    log.debug("Enter searchUsers method :: query [{}]", query);
    List<TUser> users =
        userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            query, query);
    log.debug("Exit searchUsers method");
    return users.stream().map(userMapper::toDto).collect(Collectors.toList());
  }

  /** Performs complex multi-field search based on dynamic criteria. */
  @Override
  public Page<TUserDTO> advancedSearch(AdvanceSearchCriteria criteria) {
    log.debug("Entering advancedSearch for Users");

    Pageable pageable;
    if (criteria == null) {
      pageable = PageRequest.of(0, 20);
      criteria = new AdvanceSearchCriteria();
      criteria.setFilters(new ArrayList<>());
    } else {
      pageable = PageRequest.of(criteria.getPageNumber(), criteria.getPageSize());
      if (criteria.getFilters() == null) {
        criteria.setFilters(new ArrayList<>());
      }
    }

    List<AdvanceSearchCriteria.Filter> filters = criteria.getFilters();

    // Add default filters
    commonUtil.addIsEnabledFilter(filters);

    // Generate Query
    CriteriaQuery<TUser> criteriaQuery =
        (CriteriaQuery<TUser>) commonUtil.getJpaQuery(filters, TUser.class);

    // Custom OrderBy extension
    Root<TUser> root = (Root<TUser>) criteriaQuery.getRoots().iterator().next();
    criteriaQuery.orderBy(entityManager.getCriteriaBuilder().desc(root.get("createdDate")));

    // Execute Query
    List<TUser> userList =
        entityManager
            .createQuery(criteriaQuery)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

    // Get Total Count
    long totalCount = commonUtil.getTotalCount(TUser.class, filters);

    log.debug("Converting fetched Users to DTOs.");
    List<TUserDTO> dtoList = userList.stream().map(userMapper::toDto).collect(Collectors.toList());

    return new org.springframework.data.domain.PageImpl<>(dtoList, pageable, totalCount);
  }

  /**
   * Registers a new user. Handles both Postgres persistence and Keycloak registration. Emits a
   * UserUpdatedEvent for indexing in Elasticsearch.
   */
  @Override
  public void save(TUserDTO userDTO, String clientName) {
    log.debug("Enter save method :: userDTO [{}] :: clientName [{}]", userDTO, clientName);

    if (userRepository.existsByEmail(userDTO.getEmail())) {
      log.debug("Duplicate User found to save User Request ::  [{}]", userDTO);
      throw new ConflictException(ErrorKeys.USER_ALREADY_EXISTS_WITH_THIS_EMAIL);
    }

    TUser user = userMapper.toEntity(userDTO);
    log.info("user is :: [{}]", user);
    UUID keyCloakUserId = UUID.fromString(keyCloakUtil.saveUser(userDTO, clientName, ROLE_USER));
    log.debug("Key Cloak user id is :: [{}]", keyCloakUserId);
    user.setKeycloakUserId(keyCloakUserId);

    // Normalize email and save
    user.setEmail(user.getEmail().toLowerCase());
    user = userRepository.save(user);

    // Emit synchronization event
    syncUserToSearchService(user, ACTION_CREATE);

    log.debug("Exit save method .");
  }

  /**
   * Retrieves public user details by internal ID. Result is cached in Redis for high-frequency
   * access.
   */
  @Override
  @Cacheable(value = "users", key = "#userId")
  public TUserDTO getUserDetails(UUID userId) {
    log.debug("Fetching user details for {} from database", userId);
    Optional<TUser> optionalUser = userRepository.findById(userId);
    TUser existingUser =
        optionalUser.orElseThrow(() -> new EntityNotFoundException(ERROR_USER_NOT_FOUND));
    return userMapper.toDto(existingUser);
  }

  /** Retrieves the user profile associated with the current authentication token. */
  @Override
  public TUserDTO getUserDetailsByAuthentication(Authentication authentication) {
    log.debug("Enter getUserDetails method :: [{}]", authentication);

    TUser existingUser =
        userRepository
            .findById(UUID.fromString(authentication.getName()))
            .orElseThrow(() -> new EntityNotFoundException(ERROR_USER_NOT_FOUND));

    return userMapper.toDto(existingUser);
  }

  /** Returns a user by ID if not deleted. */
  @Override
  @Transactional(readOnly = true)
  public Optional<TUserDTO> findOne(UUID id) {
    log.debug("Enter findOne method :: id [{}]", id);
    Optional<TUser> user = userRepository.findByIdAndIsDeletedFalse(id);
    return user.map(userMapper::toDto);
  }

  /** Soft-deletes a user and removes them from Keycloak. */
  @Override
  @CacheEvict(value = "users", key = "#id")
  public void delete(String clientName, UUID id) {
    log.debug("Enter delete method  :: client name [{}] :: id [{}]", clientName, id);
    TUser user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(ERROR_USER_NOT_FOUND + ": " + id));
    String keyCloakId = String.valueOf(user.getKeycloakUserId());
    keyCloakUtil.deleteUserFromKeycloak(clientName, keyCloakId);

    // Sync with other services (Legacy Action)
    syncUserToSearchService(user, ACTION_DELETE);

    // Soft delete the user (This will cascade to posts, comments, etc. if mapped correctly)
    user.setIsDeleted(true);
    userRepository.save(user);

    // Publish dedicated UserDeletedEvent
    try {
      UserDeletedEvent deleteEvent =
          UserDeletedEvent.builder()
              .userId(user.getId().toString())
              .email(user.getEmail())
              .timestamp(System.currentTimeMillis())
              .build();
      kafkaTemplate.send(userDeletedTopic, user.getId().toString(), deleteEvent);
      log.info("Published UserDeletedEvent for user: {}", user.getId());
    } catch (Exception e) {
      log.error("Failed to publish UserDeletedEvent: {}", e.getMessage());
    }
  }

  /** Retrieves a paginated list of all active users. */
  @Override
  public Page<TUserDTO> getAllUserDetail(Pageable pageable) {
    log.debug("Enter getAllUserDetail method .");
    Page<TUser> userDetails = userRepository.findAllByIsDeletedFalse(pageable);
    return userDetails.map(userMapper::toDto);
  }

  /**
   * Updates user password in Keycloak. Note: Local password storage is removed for security
   * compliance.
   */
  @Override
  public void resetPassword(ChangePassword changePasswordDTO, String clientName) {
    log.debug("Enter resetPassword method for email: {}", changePasswordDTO.getEmailId());
    String newPassword = changePasswordDTO.getNewPassword();
    String confirmPassword = changePasswordDTO.getConfirmPassword();

    if (!newPassword.equals(confirmPassword)) {
      throw new ValidationException(ErrorKeys.PASSWORD_AND_CONFIRM_PASSWORD_DO_NOT_MATCH);
    }

    TUser user = userRepository.findByEmailAndIsDeletedFalse(changePasswordDTO.getEmailId());
    if (user == null) {
      throw new ResourceNotFoundException(ERROR_USER_NOT_FOUND);
    }

    String keycloakId = user.getKeycloakUserId().toString();
    keyCloakUtil.updateUserPassword(keycloakId, newPassword, clientName);
    log.debug("Exit resetPassword method .");
  }

  /**
   * Updates core user details and/or profile image. Evicts the identity cache to ensure
   * consistency.
   */
  @Override
  @CacheEvict(value = "users", key = "#userId")
  public void updateUserById(UUID userId, MultipartFile image, TUserDTO userDTO)
      throws IOException {
    log.debug("Enter updateUserById method  :: userId [{}]", userId);
    TUser existingUser =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(ERROR_USER_NOT_FOUND));

    existingUser.setFirstName(userDTO.getFirstName());
    existingUser.setLastName(userDTO.getLastName());

    if (image != null && !image.isEmpty()) {
      String fileName = fileStorageService.storeFile(image);
      existingUser.setProfileImageUrl(fileName);
    }

    existingUser = userRepository.save(existingUser);

    // Sync updates to Elasticsearch
    syncUserToSearchService(existingUser, ACTION_UPDATE);

    TUserDTO updatedUser = userMapper.toDto(existingUser);
    keyCloakUtil.updateUserDetailsInKeycloak(updatedUser, keycloakDemoClient);
  }

  private void syncUserToSearchService(TUser user, String action) {
    UserUpdatedEvent event =
        UserUpdatedEvent.builder()
            .id(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .profileImageUrl(user.getProfileImageUrl())
            .updatedAt(System.currentTimeMillis())
            .action(action)
            .build();

    // If it's a new user, we might not have profile info yet, but we should at least try to get defaults
    // Search service handles nulls gracefully
    try {
      kafkaTemplate.send(TOPIC_USER_UPDATED, user.getId().toString(), event);
      log.info("Synced user {} to search service with action {}", user.getId(), action);
    } catch (Exception e) {
      log.error("Failed to sync user {} to search service: {}", user.getId(), e.getMessage());
    }
  }

  /** Sends a password reset link to the user's email. */
  @Override
  public String sendForgotLink(String email) throws Exception {
    TUser tUser = userRepository.findByEmailAndIsDeletedFalse(email);
    if (tUser == null) {
      throw new ResourceNotFoundException(ERROR_USER_NOT_FOUND);
    }
    String token = CryptUtil.encrypt(email + "~" + System.currentTimeMillis());
    String url = createPasswordUrl + token;
    // Send Forgot Password link on Mail
    asyncCalls.sendForgotLink(tUser.getFirstName(), tUser.getEmail(), url);
    return url;
  }

  /** Handles the password reset process for a user based on a provided token. */
  @Override
  public void forgotPassword(String token, ChangePassword changePassword) throws Exception {
    String decryptToken = CryptUtil.decrypt(token);
    String[] parts = decryptToken.split("~");
    if (parts.length != 2) {
      throw new ValidationException(INVALID_TOKEN_FORMAT);
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

  /** Saves the uploaded image for the authenticated user. */
  @Override
  public void saveImage(MultipartFile file, Authentication authentication) throws IOException {

    String contentType = file.getContentType();
    long File_Size = Long.parseLong(MAX_FILE_SIZE);
    if (file.getSize() > File_Size) {
      throw new ValidationException(FILE_SIZE_EXCEEDED);
    } else if (contentType == null || !ALLOWED_FILE_TYPES.contains(contentType)) {
      throw new ValidationException(INVALID_FILE_FORMAT);
    }

    TUser user =
        userRepository
            .findByKeycloakUserId(UUID.fromString(authentication.getName()))
            .orElseThrow(() -> new EntityNotFoundException(ERROR_USER_NOT_FOUND));

    String fileName = fileStorageService.storeFile(file);
    user.setProfileImageUrl(fileName);
    userRepository.save(user);
  }

  /** Retrieves the image for the authenticated user. */
  @Override
  public byte[] getImage(Authentication authentication) {
    return null;
  }

  /** Finds and returns a user by their Keycloak ID. */
  @Override
  public TUserDTO findUserByKeyCloakId(UUID keyCloakId) {
    log.info("Entering findUserByKeyCloakId with keyCloakId: {}", keyCloakId);
    TUser user = userRepository.findByKeycloakUserId(keyCloakId).get();
    TUserDTO userDTO = userMapper.toDto(user);
    log.info("Exiting findUserByKeyCloakId with result: {}", userDTO);
    return userDTO;
  }

  @Override
  public PrivacySettingsDTO getPrivacySettings(UUID userId) {
    PrivacySettings settings =
        privacySettingsRepository
            .findById(userId)
            .orElseGet(
                () -> {
                  PrivacySettings defaultSettings =
                      PrivacySettings.builder().userId(userId).build();
                  return privacySettingsRepository.save(defaultSettings);
                });

    return PrivacySettingsDTO.builder()
        .profileVisibility(settings.getProfileVisibility())
        .showEmail(settings.isShowEmail())
        .showConnections(settings.isShowConnections())
        .allowMessagesFrom(settings.getAllowMessagesFrom())
        .build();
  }

  @Override
  public void updatePrivacySettings(UUID userId, PrivacySettingsDTO dto) {
    PrivacySettings settings =
        privacySettingsRepository
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException(ERROR_USER_NOT_FOUND));

    settings.setProfileVisibility(dto.getProfileVisibility());
    settings.setShowEmail(dto.isShowEmail());
    settings.setShowConnections(dto.isShowConnections());
    settings.setAllowMessagesFrom(dto.getAllowMessagesFrom());

    privacySettingsRepository.save(settings);
  }

  @Override
  public void blockUser(UUID blockerId, UUID blockedId) {
    if (blockerId.equals(blockedId)) {
      throw new ValidationException(ERROR_CANNOT_BLOCK_SELF);
    }

    if (!userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
      UserBlock block = UserBlock.builder().blockerId(blockerId).blockedId(blockedId).build();
      userBlockRepository.save(block);
    }
  }

  @Override
  public void unblockUser(UUID blockerId, UUID blockedId) {
    userBlockRepository
        .findByBlockerIdAndBlockedId(blockerId, blockedId)
        .ifPresent(userBlockRepository::delete);
  }

  @Override
  public boolean isBlocked(UUID blockerId, UUID blockedId) {
    return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
  }

  @Override
  public List<TUserDTO> getBlockedUsers(UUID blockerId) {
    return userBlockRepository.findByBlockerId(blockerId).stream()
        .map(
            block -> {
              TUser user =
                  userRepository
                      .findById(block.getBlockedId())
                      .orElseThrow(() -> new EntityNotFoundException(ERROR_USER_NOT_FOUND));
              return userMapper.toDto(user);
            })
        .collect(java.util.stream.Collectors.toList());
  }

  @Override
  public List<TUserDTO> getUsersByIds(List<UUID> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Collections.emptyList();
    }
    return userRepository.findAllById(userIds).stream()
        .map(userMapper::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public void syncAllUsersToSearch() {
    log.info("Starting manual re-sync of all users to search service");
    List<TUser> allUsers = userRepository.findAllByIsDeletedFalse(Pageable.unpaged()).getContent();
    for (TUser user : allUsers) {
        syncUserToSearchService(user, ACTION_UPDATE);
    }
    log.info("Finished re-sync of {} users", allUsers.size());
  }
}
