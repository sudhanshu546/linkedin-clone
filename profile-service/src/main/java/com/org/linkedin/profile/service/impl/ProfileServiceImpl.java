package com.org.linkedin.profile.service.impl;

import static com.org.linkedin.utility.ProjectConstants.TOPIC_USER_UPDATED;
import com.org.linkedin.domain.Profile;
import com.org.linkedin.domain.ProfileSkill;
import com.org.linkedin.domain.Recommendation;
import com.org.linkedin.domain.Skill;
import com.org.linkedin.dto.ProfileDTO;
import com.org.linkedin.dto.ProfileSkillDTO;
import com.org.linkedin.dto.RecommendationDTO;
import com.org.linkedin.dto.event.ProfileViewedEvent;
import com.org.linkedin.dto.event.UserUpdatedEvent;
import com.org.linkedin.dto.user.TUserDTO;
import com.org.linkedin.profile.mapper.ProfileMapper;
import com.org.linkedin.profile.repo.*;
import com.org.linkedin.profile.service.ProfileService;
import com.org.linkedin.utility.client.UserService;
import com.org.linkedin.utility.service.AdvanceSearchCriteria;
import com.org.linkedin.utility.service.CommonUtil;
import com.org.linkedin.utility.service.KafkaEventPublisher;
import com.org.linkedin.utility.storage.FileStorageService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementation of ProfileService managing professional profiles, skills, and recommendations.
 * Uses Redis caching to optimize frequent profile lookups.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {
  private final ProfileMapper profileMapper;
  private final ProfileRepo profileRepo;
  private final SkillRepository skillRepository;
  private final ProfileSkillRepository profileSkillRepository;
  private final RecommendationRepository recommendationRepository;
  private final ConnectionRepository connectionRepository;
  private final UserService userService;
  private final KafkaEventPublisher kafkaEventPublisher;
  private final FileStorageService fileStorageService;

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @PersistenceContext private final EntityManager entityManager;
  private final CommonUtil commonUtil;

  @Value("${kafka.topics.profile-viewed}")
  private String profileViewedTopic;

  /**
   * Retrieves a professional profile by User UUID.
   * Caches the result in Redis to minimize database load.
   *
   * @param userId The unique identifier of the user.
   * @return The ProfileDTO containing professional details.
   */
  @Override
  @Cacheable(value = "profiles", key = "#userId")
  public ProfileDTO getByUserId(UUID userId) {
    log.debug("Fetching profile for user {} from database", userId);
    Profile profile = profileRepo.findByUserId(userId);

    // Publish Profile Viewed Event (Analytics)
    String currentUserName = SecurityContextHolder.getContext().getAuthentication().getName();
    if (currentUserName != null && !currentUserName.equals("anonymousUser")) {
      try {
        UUID viewerId =
            userService
                .getUserByKeyCloakId(UUID.fromString(currentUserName))
                .getBody()
                .getData()
                .getId();
        if (!viewerId.equals(userId)) {
          ProfileViewedEvent event =
              ProfileViewedEvent.builder()
                  .viewerId(viewerId)
                  .profileOwnerId(userId)
                  .timestamp(LocalDateTime.now())
                  .build();
          kafkaEventPublisher.publishEvent(profileViewedTopic, event);
        }
      } catch (Exception e) {
        log.error("Failed to publish profile view event: {}", e.getMessage());
      }
      }

      return profileMapper.toDto(profile);
  }

  /**
   * Updates or creates a professional profile.
   * Evicts the cached profile entry to ensure consistency on next read.
   */
  @Override
  @CacheEvict(value = "profiles", key = "#userId")
  public ProfileDTO saveOrUpdate(UUID userId, ProfileDTO profileDTO) {
    log.debug("Updating profile for user {} and evicting cache", userId);
    TUserDTO tUserDTO = userService.getUserByKeyCloakId(userId).getBody().getData();
    Profile existingProfile = profileRepo.findByUserId(tUserDTO.getId());
    Profile profile;
    if (existingProfile != null) {
      profileMapper.partialUpdate(existingProfile, profileDTO);
      profile = existingProfile;
    } else {
      profile = profileMapper.toEntity(profileDTO);
      profile.setUserId(tUserDTO.getId());
    }
    profile = profileRepo.save(profile);

    // Emit event for search-service and other consumers
    syncToSearchService(tUserDTO, profile);

    return profileMapper.toDto(profile);
  }

  private void syncToSearchService(TUserDTO tUserDTO, Profile profile) {
    kafkaTemplate.send(
        TOPIC_USER_UPDATED,
        tUserDTO.getId().toString(),
        UserUpdatedEvent.builder()
            .id(tUserDTO.getId())
            .firstName(tUserDTO.getFirstName())
            .lastName(tUserDTO.getLastName())
            .email(tUserDTO.getEmail())
            .headline(profile.getHeadline())
            .skills(profile.getSkills())
            .city(profile.getCity())
            .state(profile.getState())
            .currentCompany(profile.getCurrentCompany())
            .designation(profile.getDesignation())
            .profileImageUrl(tUserDTO.getProfileImageUrl())
            .updatedAt(System.currentTimeMillis())
            .action("UPDATE")
            .build());
  }

  /**
   * Adds a skill to a user's professional profile.
   * Evicts the profile cache to reflect the updated skills list.
   */
  @Override
  @Transactional
  @CacheEvict(value = "profiles", key = "#userId")
  public ProfileSkillDTO addSkill(
      UUID userId, String skillName, String category) {
    TUserDTO tUserDTO = userService.getUserByKeyCloakId(userId).getBody().getData();
    Profile profile = profileRepo.findByUserId(tUserDTO.getId());
    if (profile == null) throw new RuntimeException("Profile not found");

    Skill skill =
        skillRepository
            .findByNameIgnoreCase(skillName)
            .orElseGet(
                () ->
                    skillRepository.save(
                        Skill.builder()
                            .name(skillName)
                            .category(category)
                            .build()));

    ProfileSkill profileSkill =
        profileSkillRepository
            .findByProfileIdAndSkillId(profile.getId(), skill.getId())
            .orElseGet(
                () ->
                    profileSkillRepository.save(
                        ProfileSkill.builder()
                            .profile(profile)
                            .skill(skill)
                            .build()));

    syncSkillsString(profile);
    return profileMapper.toDto(profileSkill);
  }

  @Override
  @Transactional
  @CacheEvict(value = "profiles", key = "#userId")
  public void removeSkill(UUID userId, UUID profileSkillId) {
    ProfileSkill profileSkill =
        profileSkillRepository
            .findById(profileSkillId)
            .orElseThrow(() -> new RuntimeException("Skill not found"));

    Profile profile = profileSkill.getProfile();
    profileSkillRepository.delete(profileSkill);
    syncSkillsString(profile);
  }

  @Override
  @Transactional
  public void endorseSkill(UUID endorserId, UUID profileSkillId) {
    ProfileSkill profileSkill =
        profileSkillRepository
            .findById(profileSkillId)
            .orElseThrow(() -> new RuntimeException("Skill not found"));

    profileSkill.setEndorsementCount(profileSkill.getEndorsementCount() + 1);
    profileSkillRepository.save(profileSkill);
  }

  private void syncSkillsString(Profile profile) {
    List<ProfileSkill> skills =
        profileSkillRepository.findByProfileId(profile.getId());
    String skillsString =
        skills.stream().map(ps -> ps.getSkill().getName()).collect(Collectors.joining(", "));
    profile.setSkills(skillsString);
    profileRepo.save(profile);

    // Update Search Service
    TUserDTO tUserDTO = userService.getUserById(profile.getUserId()).getBody().getData();
    kafkaTemplate.send(
        TOPIC_USER_UPDATED,
        tUserDTO.getId().toString(),
        UserUpdatedEvent.builder()
            .id(tUserDTO.getId())
            .firstName(tUserDTO.getFirstName())
            .lastName(tUserDTO.getLastName())
            .email(tUserDTO.getEmail())
            .headline(profile.getHeadline())
            .skills(profile.getSkills())
            .city(profile.getCity())
            .state(profile.getState())
            .currentCompany(profile.getCurrentCompany())
            .designation(profile.getDesignation())
            .profileImageUrl(tUserDTO.getProfileImageUrl())
            .updatedAt(System.currentTimeMillis())
            .action("UPDATE")
            .build());
  }

  // Recommendations Implementation
  @Override
  public RecommendationDTO requestRecommendation(
      UUID requesterId, UUID authorId, String message) {
    TUserDTO authorDTO = userService.getUserByKeyCloakId(authorId).getBody().getData();
    TUserDTO requesterDTO = userService.getUserByKeyCloakId(requesterId).getBody().getData();
    Profile requesterProfile = profileRepo.findByUserId(requesterDTO.getId());

    Recommendation rec =
        recommendationRepository.save(
            Recommendation.builder()
                .profile(requesterProfile)
                .authorId(authorDTO.getId())
                .content("PENDING_SUBMISSION: " + message)
                .status("PENDING")
                .build());

    return profileMapper.toDto(rec);
  }

  @Override
  public RecommendationDTO submitRecommendation(
      UUID authorId, UUID recommendationId, String content) {
    TUserDTO authorDTO = userService.getUserByKeyCloakId(authorId).getBody().getData();
    Recommendation rec =
        recommendationRepository
            .findById(recommendationId)
            .orElseThrow(() -> new RuntimeException("Recommendation request not found"));

    if (!rec.getAuthorId().equals(authorDTO.getId())) {
      throw new RuntimeException("Not authorized to submit this recommendation");
    }

    rec.setContent(content);
    rec.setStatus("ACCEPTED");
    return profileMapper.toDto(recommendationRepository.save(rec));
  }

  @Override
  public void updateRecommendationStatus(UUID userId, UUID recommendationId, String status) {
    TUserDTO userDTO = userService.getUserByKeyCloakId(userId).getBody().getData();
    Recommendation rec =
        recommendationRepository
            .findById(recommendationId)
            .orElseThrow(() -> new RuntimeException("Recommendation not found"));

    if (!rec.getProfile().getUserId().equals(userDTO.getId())) {
      throw new RuntimeException("Not authorized");
    }

    rec.setStatus(status);
    recommendationRepository.save(rec);
  }

  @Override
  public List<ProfileDTO> getPeopleYouMayKnow(UUID userId) {
    TUserDTO tUserDTO = userService.getUserByKeyCloakId(userId).getBody().getData();
    List<UUID> suggestedUserIds = connectionRepository.findPeopleYouMayKnow(tUserDTO.getId());

    return suggestedUserIds.stream()
        .map(profileRepo::findByUserId)
        .filter(java.util.Objects::nonNull)
        .map(profileMapper::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public List<ProfileDTO> searchProfiles(
      String query,
      String city,
      String state,
      String company,
      String headline,
      String sortBy,
      String sortOrder) {
    List<Profile> results = profileRepo.searchProfiles(query, city, state, company, headline, sortBy);
    return profileMapper.toDto(results);
  }

  @Override
  public List<ProfileDTO> advancedSearch(AdvanceSearchCriteria criteria) {
    log.debug("Entering advancedSearch for Profiles");
    
    if (criteria == null) {
        criteria = new AdvanceSearchCriteria();
        criteria.setPageNumber(0);
        criteria.setPageSize(20);
        criteria.setFilters(new ArrayList<>());
    }
    
    List<AdvanceSearchCriteria.Filter> filters = criteria.getFilters();
    if (filters == null) {
        filters = new ArrayList<>();
        criteria.setFilters(filters);
    }
    
    commonUtil.addIsEnabledFilter(filters);

    CriteriaQuery<Profile> criteriaQuery = (CriteriaQuery<Profile>) commonUtil.getJpaQuery(filters, Profile.class);
    
    List<Profile> results = entityManager.createQuery(criteriaQuery)
        .setFirstResult(criteria.getPageNumber() * criteria.getPageSize())
        .setMaxResults(criteria.getPageSize())
        .getResultList();
    return profileMapper.toDto(results);
  }

  @Override
  @Transactional
  public String updateCoverImage(UUID userId, MultipartFile image) throws IOException {
    TUserDTO tUserDTO = userService.getUserByKeyCloakId(userId).getBody().getData();
    Profile profile = profileRepo.findByUserId(tUserDTO.getId());
    if (profile == null) throw new RuntimeException("Profile not found");

    if (profile.getCoverImageUrl() != null) {
      fileStorageService.deleteFile(profile.getCoverImageUrl());
    }

    String imageUrl = fileStorageService.storeFile(image);
    profile.setCoverImageUrl(imageUrl);
    profileRepo.save(profile);
    return imageUrl;
  }
}
