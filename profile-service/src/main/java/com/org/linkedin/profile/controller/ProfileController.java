package com.org.linkedin.profile.controller;

import com.org.linkedin.domain.Education;
import com.org.linkedin.domain.Experience;
import com.org.linkedin.domain.Profile;
import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.ProfileDTO;
import com.org.linkedin.profile.domain.Post;
import com.org.linkedin.profile.domain.ProfileView;
import com.org.linkedin.profile.repo.*;
import com.org.linkedin.profile.service.ProfileService;
import com.org.linkedin.utility.client.UserService;
import com.org.linkedin.utility.service.AdvanceSearchCriteria;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${apiPrefix}/profiles")
@RequiredArgsConstructor
public class ProfileController {

  private final ProfileService profileService;
  private final UserService userService;
  private final ProfileViewRepository profileViewRepository;
  private final PostRepository postRepository;
  private final ProfileRepo profileRepo;
  private final ExperienceRepository experienceRepository;
  private final EducationRepository educationRepository;
  private final DailyProfileViewRepository dailyProfileViewRepository;

  /** Get logged-in user's profile */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<ProfileDTO>> getMyProfile(Authentication authentication) {
    UUID userId = UUID.fromString(authentication.getName());

    var userResponse = userService.getUserByKeyCloakId(userId);
    if (userResponse == null
        || userResponse.getBody() == null
        || userResponse.getBody().getData() == null) {
      throw new RuntimeException("User not found in user-service");
    }

    UUID internalUserId = userResponse.getBody().getData().getId();
    ProfileDTO profile = profileService.getByUserId(internalUserId);
    return ResponseEntity.ok(ApiResponse.success("Success", profile));
  }

  @GetMapping("/me/views")
  public ResponseEntity<ApiResponse<List<ProfileView>>> getMyProfileViews(
      Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();
    List<ProfileView> views =
        profileViewRepository.findByProfileOwnerIdOrderByViewedAtDesc(internalUserId);
    return ResponseEntity.ok(ApiResponse.success("Success", views));
  }

  @GetMapping("/me/views/count")
  public ResponseEntity<ApiResponse<Long>> getMyProfileViewCount(Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();
    long count = profileViewRepository.countByProfileOwnerId(internalUserId);
    return ResponseEntity.ok(ApiResponse.success("Success", count));
  }

  @GetMapping("/me/views/trends")
  public ResponseEntity<ApiResponse<List<com.org.linkedin.profile.domain.DailyProfileView>>>
      getMyProfileViewTrends(Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();

    java.time.LocalDate sevenDaysAgo = java.time.LocalDate.now().minusDays(7);
    List<com.org.linkedin.profile.domain.DailyProfileView> trends =
        dailyProfileViewRepository.findByProfileOwnerIdAndViewDateAfterOrderByViewDateAsc(
            internalUserId, sevenDaysAgo);

    return ResponseEntity.ok(ApiResponse.success("Success", trends));
  }

  @GetMapping("/me/views/demographics")
  public ResponseEntity<ApiResponse<Map<String, Map<String, Long>>>> getMyProfileDemographics(
      Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();

    List<ProfileView> views =
        profileViewRepository.findByProfileOwnerIdOrderByViewedAtDesc(internalUserId);

    Map<String, Long> titles =
        views.stream()
            .filter(v -> v.getViewerDesignation() != null)
            .collect(
                java.util.stream.Collectors.groupingBy(
                    ProfileView::getViewerDesignation, java.util.stream.Collectors.counting()));

    Map<String, Long> companies =
        views.stream()
            .filter(v -> v.getViewerCompany() != null)
            .collect(
                java.util.stream.Collectors.groupingBy(
                    ProfileView::getViewerCompany, java.util.stream.Collectors.counting()));

    Map<String, Map<String, Long>> result = new HashMap<>();
    result.put("titles", titles);
    result.put("companies", companies);

    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /** Get posts authored by the logged-in user */
  @GetMapping("/me/posts")
  public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<Post>>> getMyPosts(
      Authentication authentication, org.springframework.data.domain.Pageable pageable) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();
    org.springframework.data.domain.Page<Post> posts =
        postRepository.findByAuthorIdOrderByCreatedDateDesc(internalUserId, pageable);
    return ResponseEntity.ok(ApiResponse.success("Success", posts));
  }

  /** Get posts authored by a specific internal User ID */
  @GetMapping("/{userId}/posts")
  public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<Post>>> getPostsByUser(
      @PathVariable UUID userId, org.springframework.data.domain.Pageable pageable) {
    org.springframework.data.domain.Page<Post> posts =
        postRepository.findByAuthorIdOrderByCreatedDateDesc(userId, pageable);
    return ResponseEntity.ok(ApiResponse.success("Success", posts));
  }

  /** Create or update logged-in user's profile */
  @PostMapping
  @PutMapping
  public ResponseEntity<ApiResponse<ProfileDTO>> saveOrUpdateProfile(
      Authentication authentication, @Valid @RequestBody ProfileDTO profileDTO) {
    UUID userId = UUID.fromString(authentication.getName());
    ProfileDTO profile = profileService.saveOrUpdate(userId, profileDTO);
    return ResponseEntity.ok(ApiResponse.success("Success", profile));
  }

  /** Public profile view (LinkedIn style) */
  @GetMapping("/{userId}")
  public ResponseEntity<ApiResponse<ProfileDTO>> getProfileByUserId(@PathVariable UUID userId) {
    ProfileDTO profile = profileService.getByUserId(userId);
    return ResponseEntity.ok(ApiResponse.success("Success", profile));
  }

  @GetMapping("/search")
  public ResponseEntity<ApiResponse<List<ProfileDTO>>> searchProfiles(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String company,
      @RequestParam(required = false) String headline,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String sortOrder) {
    List<ProfileDTO> profiles =
        profileService.searchProfiles(query, city, state, company, headline, sortBy, sortOrder);
    return ResponseEntity.ok(ApiResponse.success("Success", profiles));
  }

  @PostMapping("/advanced-search")
  public ResponseEntity<ApiResponse<List<ProfileDTO>>> advancedSearch(
      @RequestBody AdvanceSearchCriteria criteria) {
    List<ProfileDTO> profiles = profileService.advancedSearch(criteria);
    return ResponseEntity.ok(ApiResponse.success("Success", profiles));
  }

  @PostMapping("/cover-image")
  public ResponseEntity<ApiResponse<String>> updateCoverImage(
      Authentication authentication,
      @RequestParam("image") org.springframework.web.multipart.MultipartFile image)
      throws java.io.IOException {
    UUID userId = UUID.fromString(authentication.getName());
    String imageUrl = profileService.updateCoverImage(userId, image);
    return ResponseEntity.ok(ApiResponse.success("Success", imageUrl));
  }

  @PostMapping("/experience")
  public ResponseEntity<ApiResponse<Experience>> addExperience(
      Authentication authentication, @RequestBody Experience experience) {
    UUID userId = UUID.fromString(authentication.getName());
    Profile profile =
        profileRepo.findByUserId(
            userService.getUserByKeyCloakId(userId).getBody().getData().getId());
    experience.setProfile(profile);
    Experience result = experienceRepository.save(experience);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @PutMapping("/experience/{id}")
  public ResponseEntity<ApiResponse<Experience>> updateExperience(
      Authentication authentication,
      @PathVariable UUID id,
      @RequestBody Experience experienceDetails) {
    UUID userId = UUID.fromString(authentication.getName());
    Profile profile =
        profileRepo.findByUserId(
            userService.getUserByKeyCloakId(userId).getBody().getData().getId());
    Experience experience =
        experienceRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Experience not found"));
    if (!experience.getProfile().getId().equals(profile.getId())) {
      throw new RuntimeException("Unauthorized");
    }
    experience.setPosition(experienceDetails.getPosition());
    experience.setCompany(experienceDetails.getCompany());
    experience.setStartDate(experienceDetails.getStartDate());
    experience.setEndDate(experienceDetails.getEndDate());
    experience.setCurrentlyWorking(experienceDetails.isCurrentlyWorking());
    experience.setDescription(experienceDetails.getDescription());
    Experience result = experienceRepository.save(experience);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @DeleteMapping("/experience/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteExperience(
      Authentication authentication, @PathVariable UUID id) {
    UUID userId = UUID.fromString(authentication.getName());
    Profile profile =
        profileRepo.findByUserId(
            userService.getUserByKeyCloakId(userId).getBody().getData().getId());
    Experience experience =
        experienceRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Experience not found"));
    if (!experience.getProfile().getId().equals(profile.getId())) {
      throw new RuntimeException("Unauthorized");
    }
    experienceRepository.delete(experience);
    return ResponseEntity.ok(ApiResponse.success("Experience deleted", null));
  }

  @PostMapping("/education")
  public ResponseEntity<ApiResponse<Education>> addEducation(
      Authentication authentication, @RequestBody Education education) {
    UUID userId = UUID.fromString(authentication.getName());
    Profile profile =
        profileRepo.findByUserId(
            userService.getUserByKeyCloakId(userId).getBody().getData().getId());
    education.setProfile(profile);
    Education result = educationRepository.save(education);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @PutMapping("/education/{id}")
  public ResponseEntity<ApiResponse<Education>> updateEducation(
      Authentication authentication,
      @PathVariable UUID id,
      @RequestBody Education educationDetails) {
    UUID userId = UUID.fromString(authentication.getName());
    Profile profile =
        profileRepo.findByUserId(
            userService.getUserByKeyCloakId(userId).getBody().getData().getId());
    Education education =
        educationRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Education not found"));
    if (!education.getProfile().getId().equals(profile.getId())) {
      throw new RuntimeException("Unauthorized");
    }
    education.setInstitution(educationDetails.getInstitution());
    education.setDegree(educationDetails.getDegree());
    education.setFieldOfStudy(educationDetails.getFieldOfStudy());
    education.setStartDate(educationDetails.getStartDate());
    education.setEndDate(educationDetails.getEndDate());
    Education result = educationRepository.save(education);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @DeleteMapping("/education/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteEducation(
      Authentication authentication, @PathVariable UUID id) {
    UUID userId = UUID.fromString(authentication.getName());
    Profile profile =
        profileRepo.findByUserId(
            userService.getUserByKeyCloakId(userId).getBody().getData().getId());
    Education education =
        educationRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Education not found"));
    if (!education.getProfile().getId().equals(profile.getId())) {
      throw new RuntimeException("Unauthorized");
    }
    educationRepository.delete(education);
    return ResponseEntity.ok(ApiResponse.success("Education deleted", null));
  }

  // --- SKILLS ---
  @PostMapping("/skills")
  public ResponseEntity<ApiResponse<com.org.linkedin.dto.ProfileSkillDTO>> addSkill(
      Authentication authentication,
      @RequestParam String name,
      @RequestParam(required = false) String category) {
    UUID userId = UUID.fromString(authentication.getName());
    com.org.linkedin.dto.ProfileSkillDTO result = profileService.addSkill(userId, name, category);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @DeleteMapping("/skills/{id}")
  public ResponseEntity<ApiResponse<Void>> removeSkill(
      Authentication authentication, @PathVariable UUID id) {
    UUID userId = UUID.fromString(authentication.getName());
    profileService.removeSkill(userId, id);
    return ResponseEntity.ok(ApiResponse.success("Skill removed", null));
  }

  @PostMapping("/skills/{id}/endorse")
  public ResponseEntity<ApiResponse<Void>> endorseSkill(
      Authentication authentication, @PathVariable UUID id) {
    UUID viewerKeycloakId = UUID.fromString(authentication.getName());
    UUID internalViewerId =
        userService.getUserByKeyCloakId(viewerKeycloakId).getBody().getData().getId();
    profileService.endorseSkill(internalViewerId, id);
    return ResponseEntity.ok(ApiResponse.success("Skill endorsed", null));
  }

  // --- RECOMMENDATIONS ---
  @PostMapping("/recommendations/request")
  public ResponseEntity<ApiResponse<com.org.linkedin.dto.RecommendationDTO>> requestRecommendation(
      Authentication authentication, @RequestParam UUID authorId, @RequestParam String message) {
    UUID userId = UUID.fromString(authentication.getName());
    com.org.linkedin.dto.RecommendationDTO result =
        profileService.requestRecommendation(userId, authorId, message);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @PostMapping("/recommendations/{id}/submit")
  public ResponseEntity<ApiResponse<com.org.linkedin.dto.RecommendationDTO>> submitRecommendation(
      Authentication authentication, @PathVariable UUID id, @RequestBody String content) {
    UUID userId = UUID.fromString(authentication.getName());
    com.org.linkedin.dto.RecommendationDTO result =
        profileService.submitRecommendation(userId, id, content);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @PutMapping("/recommendations/{id}/status")
  public ResponseEntity<ApiResponse<Void>> updateRecommendationStatus(
      Authentication authentication, @PathVariable UUID id, @RequestParam String status) {
    UUID userId = UUID.fromString(authentication.getName());
    profileService.updateRecommendationStatus(userId, id, status);
    return ResponseEntity.ok(ApiResponse.success("Status updated", null));
  }

  @GetMapping("/suggestions")
  public ResponseEntity<ApiResponse<List<ProfileDTO>>> getPeopleYouMayKnow(
      Authentication authentication) {
    UUID userId = UUID.fromString(authentication.getName());
    List<ProfileDTO> result = profileService.getPeopleYouMayKnow(userId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            (error) -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });
    return errors;
  }
}
