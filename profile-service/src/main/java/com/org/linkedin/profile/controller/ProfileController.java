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

  /**
   * Retrieves the profile of the currently authenticated user.
   *
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with the user's ProfileDTO.
   */
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

  /**
   * Retrieves a list of profile views for the currently authenticated user.
   *
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with a list of ProfileView entities.
   */
  @GetMapping("/me/views")
  public ResponseEntity<ApiResponse<List<ProfileView>>> getMyProfileViews(
      Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();
    List<ProfileView> views =
        profileViewRepository.findByProfileOwnerIdOrderByViewedAtDesc(internalUserId);
    return ResponseEntity.ok(ApiResponse.success("Success", views));
  }

  /**
   * Retrieves the total count of profile views for the currently authenticated user.
   *
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with the total view count.
   */
  @GetMapping("/me/views/count")
  public ResponseEntity<ApiResponse<Long>> getMyProfileViewCount(Authentication authentication) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();
    long count = profileViewRepository.countByProfileOwnerId(internalUserId);
    return ResponseEntity.ok(ApiResponse.success("Success", count));
  }

  /**
   * Retrieves profile view trends (daily counts) for the last 7 days.
   *
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with a list of DailyProfileView entities.
   */
  @GetMapping("/me/views/trends")
  public ResponseEntity<ApiResponse<List<com.org.linkedin.profile.domain.DailyProfileView>>>
      getMyProfileViewTrends(
          Authentication authentication, @RequestParam(defaultValue = "7") int days) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();

    java.time.LocalDate startDate = java.time.LocalDate.now().minusDays(days);
    List<com.org.linkedin.profile.domain.DailyProfileView> trends =
        dailyProfileViewRepository.findByProfileOwnerIdAndViewDateAfterOrderByViewDateAsc(
            internalUserId, startDate);

    return ResponseEntity.ok(ApiResponse.success("Success", trends));
  }

  /**
   * Retrieves demographic data (titles and companies) of users who viewed the current user's
   * profile.
   *
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with a nested map of demographic statistics.
   */
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

  /**
   * Retrieves a paginated list of posts authored by the currently authenticated user.
   *
   * @param authentication The authenticated user security context.
   * @param pageable Pagination and sorting information.
   * @return A ResponseEntity containing an ApiResponse with a paginated list of Post entities.
   */
  @GetMapping("/me/posts")
  public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<Post>>> getMyPosts(
      Authentication authentication, org.springframework.data.domain.Pageable pageable) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getData().getId();
    org.springframework.data.domain.Page<Post> posts =
        postRepository.findByAuthorIdOrderByCreatedDateDesc(internalUserId, pageable);
    return ResponseEntity.ok(ApiResponse.success("Success", posts));
  }

  /**
   * Retrieves a paginated list of posts authored by a specific user.
   *
   * @param userId The unique internal identifier of the user.
   * @param pageable Pagination and sorting information.
   * @return A ResponseEntity containing an ApiResponse with a paginated list of Post entities.
   */
  @GetMapping("/{userId}/posts")
  public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<Post>>> getPostsByUser(
      @PathVariable UUID userId, org.springframework.data.domain.Pageable pageable) {
    org.springframework.data.domain.Page<Post> posts =
        postRepository.findByAuthorIdOrderByCreatedDateDesc(userId, pageable);
    return ResponseEntity.ok(ApiResponse.success("Success", posts));
  }

  /**
   * Creates or updates the profile of the currently authenticated user.
   *
   * @param authentication The authenticated user security context.
   * @param profileDTO The profile data transfer object containing the details to save.
   * @return A ResponseEntity containing an ApiResponse with the saved ProfileDTO.
   */
  @PostMapping
  @PutMapping
  public ResponseEntity<ApiResponse<ProfileDTO>> saveOrUpdateProfile(
      Authentication authentication, @Valid @RequestBody ProfileDTO profileDTO) {
    UUID userId = UUID.fromString(authentication.getName());
    ProfileDTO profile = profileService.saveOrUpdate(userId, profileDTO);
    return ResponseEntity.ok(ApiResponse.success("Success", profile));
  }

  /**
   * Retrieves the profile of a specific user by their internal user ID.
   *
   * @param userId The unique internal identifier of the user whose profile is to be retrieved.
   * @return A ResponseEntity containing an ApiResponse with the requested ProfileDTO.
   */
  @GetMapping("/{userId}")
  public ResponseEntity<ApiResponse<ProfileDTO>> getProfileByUserId(@PathVariable UUID userId) {
    ProfileDTO profile = profileService.getByUserId(userId);
    return ResponseEntity.ok(ApiResponse.success("Success", profile));
  }

  /**
   * Searches for profiles based on various criteria such as query, city, state, company, and
   * headline.
   *
   * @param query General search keyword.
   * @param city City to filter by.
   * @param state State to filter by.
   * @param company Company name to filter by.
   * @param headline Headline text to filter by.
   * @param sortBy Field to sort the results by.
   * @param sortOrder Order of sorting (asc/desc).
   * @return A ResponseEntity containing an ApiResponse with a list of matching ProfileDTOs.
   */
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

  /**
   * Performs an advanced search for profiles using multiple complex filter criteria.
   *
   * @param criteria The advanced search criteria DTO.
   * @return A ResponseEntity containing an ApiResponse with a list of matching ProfileDTOs.
   */
  @PostMapping("/advanced-search")
  public ResponseEntity<ApiResponse<List<ProfileDTO>>> advancedSearch(
      @RequestBody AdvanceSearchCriteria criteria) {
    List<ProfileDTO> profiles = profileService.advancedSearch(criteria);
    return ResponseEntity.ok(ApiResponse.success("Success", profiles));
  }

  /**
   * Updates the cover image for the currently authenticated user's profile.
   *
   * @param authentication The authenticated user security context.
   * @param image The multipart file containing the new cover image.
   * @return A ResponseEntity containing an ApiResponse with the URL of the uploaded cover image.
   * @throws java.io.IOException If an error occurs during file processing or storage.
   */
  @PostMapping("/cover-image")
  public ResponseEntity<ApiResponse<String>> updateCoverImage(
      Authentication authentication,
      @RequestParam("image") org.springframework.web.multipart.MultipartFile image)
      throws java.io.IOException {
    UUID userId = UUID.fromString(authentication.getName());
    String imageUrl = profileService.updateCoverImage(userId, image);
    return ResponseEntity.ok(ApiResponse.success("Success", imageUrl));
  }

  /**
   * Adds a new work experience entry to the authenticated user's profile.
   *
   * @param authentication The authenticated user security context.
   * @param experience The experience entity to be added.
   * @return A ResponseEntity containing an ApiResponse with the saved Experience entity.
   */
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

  /**
   * Updates an existing work experience entry.
   *
   * @param authentication The authenticated user security context.
   * @param id The unique identifier of the experience entry to update.
   * @param experienceDetails The updated experience details.
   * @return A ResponseEntity containing an ApiResponse with the updated Experience entity.
   */
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

  /**
   * Deletes a specific work experience entry from the user's profile.
   *
   * @param authentication The authenticated user security context.
   * @param id The unique identifier of the experience entry to delete.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
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

  /**
   * Adds a new education entry to the authenticated user's profile.
   *
   * @param authentication The authenticated user security context.
   * @param education The education entity to be added.
   * @return A ResponseEntity containing an ApiResponse with the saved Education entity.
   */
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

  /**
   * Updates an existing education entry.
   *
   * @param authentication The authenticated user security context.
   * @param id The unique identifier of the education entry to update.
   * @param educationDetails The updated education details.
   * @return A ResponseEntity containing an ApiResponse with the updated Education entity.
   */
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

  /**
   * Deletes a specific education entry from the user's profile.
   *
   * @param authentication The authenticated user security context.
   * @param id The unique identifier of the education entry to delete.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
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

  /**
   * Adds a new skill to the authenticated user's profile.
   *
   * @param authentication The authenticated user security context.
   * @param name The name of the skill.
   * @param category Optional category for the skill.
   * @return A ResponseEntity containing an ApiResponse with the added ProfileSkillDTO.
   */
  @PostMapping("/skills")
  public ResponseEntity<ApiResponse<com.org.linkedin.dto.ProfileSkillDTO>> addSkill(
      Authentication authentication,
      @RequestParam String name,
      @RequestParam(required = false) String category) {
    UUID userId = UUID.fromString(authentication.getName());
    com.org.linkedin.dto.ProfileSkillDTO result = profileService.addSkill(userId, name, category);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Removes a skill from the authenticated user's profile.
   *
   * @param authentication The authenticated user security context.
   * @param id The unique identifier of the skill to remove.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @DeleteMapping("/skills/{id}")
  public ResponseEntity<ApiResponse<Void>> removeSkill(
      Authentication authentication, @PathVariable UUID id) {
    UUID userId = UUID.fromString(authentication.getName());
    profileService.removeSkill(userId, id);
    return ResponseEntity.ok(ApiResponse.success("Skill removed", null));
  }

  /**
   * Endorses a specific skill on another user's profile.
   *
   * @param authentication The authenticated viewer's security context.
   * @param id The unique identifier of the skill entry to endorse.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @PostMapping("/skills/{id}/endorse")
  public ResponseEntity<ApiResponse<Void>> endorseSkill(
      Authentication authentication, @PathVariable UUID id) {
    UUID viewerKeycloakId = UUID.fromString(authentication.getName());
    UUID internalViewerId =
        userService.getUserByKeyCloakId(viewerKeycloakId).getBody().getData().getId();
    profileService.endorseSkill(internalViewerId, id);
    return ResponseEntity.ok(ApiResponse.success("Skill endorsed", null));
  }

  /**
   * Requests a recommendation from another user.
   *
   * @param authentication The authenticated user security context.
   * @param authorId The unique internal identifier of the user being asked for a recommendation.
   * @param message A personal message to include with the request.
   * @return A ResponseEntity containing an ApiResponse with the created RecommendationDTO.
   */
  @PostMapping("/recommendations/request")
  public ResponseEntity<ApiResponse<com.org.linkedin.dto.RecommendationDTO>> requestRecommendation(
      Authentication authentication, @RequestParam UUID authorId, @RequestParam String message) {
    UUID userId = UUID.fromString(authentication.getName());
    com.org.linkedin.dto.RecommendationDTO result =
        profileService.requestRecommendation(userId, authorId, message);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Submits a recommendation in response to a request.
   *
   * @param authentication The authenticated user security context (the recommendation author).
   * @param id The unique identifier of the recommendation request.
   * @param content The text content of the recommendation.
   * @return A ResponseEntity containing an ApiResponse with the updated RecommendationDTO.
   */
  @PostMapping("/recommendations/{id}/submit")
  public ResponseEntity<ApiResponse<com.org.linkedin.dto.RecommendationDTO>> submitRecommendation(
      Authentication authentication, @PathVariable UUID id, @RequestBody String content) {
    UUID userId = UUID.fromString(authentication.getName());
    com.org.linkedin.dto.RecommendationDTO result =
        profileService.submitRecommendation(userId, id, content);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Updates the status (e.g., PENDING, ACCEPTED, REJECTED) of a recommendation.
   *
   * @param authentication The authenticated user security context.
   * @param id The unique identifier of the recommendation.
   * @param status The new status to be applied.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @PutMapping("/recommendations/{id}/status")
  public ResponseEntity<ApiResponse<Void>> updateRecommendationStatus(
      Authentication authentication, @PathVariable UUID id, @RequestParam String status) {
    UUID userId = UUID.fromString(authentication.getName());
    profileService.updateRecommendationStatus(userId, id, status);
    return ResponseEntity.ok(ApiResponse.success("Status updated", null));
  }

  /**
   * Retrieves a list of profile suggestions for the current user ("People You May Know").
   *
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with a list of suggested ProfileDTOs.
   */
  @GetMapping("/suggestions")
  public ResponseEntity<ApiResponse<List<ProfileDTO>>> getPeopleYouMayKnow(
      Authentication authentication) {
    UUID userId = UUID.fromString(authentication.getName());
    List<ProfileDTO> result = profileService.getPeopleYouMayKnow(userId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Global exception handler for validation errors in this controller.
   *
   * @param ex The MethodArgumentNotValidException thrown during request processing.
   * @return A map of field names to validation error messages.
   */
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
