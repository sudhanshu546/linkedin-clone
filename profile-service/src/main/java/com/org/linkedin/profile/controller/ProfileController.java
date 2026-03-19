package com.org.linkedin.profile.controller;

import com.org.linkedin.domain.Education;
import com.org.linkedin.domain.Experience;
import com.org.linkedin.domain.Profile;
import com.org.linkedin.dto.BaseResponse;
import com.org.linkedin.dto.ProfileDTO;
import com.org.linkedin.profile.domain.Post;
import com.org.linkedin.profile.domain.ProfileView;
import com.org.linkedin.profile.repo.EducationRepository;
import com.org.linkedin.profile.repo.ExperienceRepository;
import com.org.linkedin.profile.repo.PostRepository;
import com.org.linkedin.profile.repo.ProfileRepo;
import com.org.linkedin.profile.repo.ProfileViewRepository;
import com.org.linkedin.profile.service.ProfileService;
import com.org.linkedin.utility.client.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /**
     * Get logged-in user's profile
     */
    @GetMapping("/me")
    public BaseResponse<ProfileDTO> getMyProfile(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        ProfileDTO profile = profileService.getByUserId(userService.getUserByKeyCloakId(userId).getBody().getResult().getId());
        return BaseResponse.<ProfileDTO>builder()
                .status(HttpStatus.OK.value())
                .result(profile)
                .build();
    }

    @GetMapping("/me/views")
    public BaseResponse<List<ProfileView>> getMyProfileViews(Authentication authentication) {
        UUID keycloakId = UUID.fromString(authentication.getName());
        UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();
        List<ProfileView> views = profileViewRepository.findByProfileOwnerIdOrderByViewedAtDesc(internalUserId);
        return BaseResponse.<List<ProfileView>>builder()
                .status(HttpStatus.OK.value())
                .result(views)
                .build();
    }

    @GetMapping("/me/views/count")
    public BaseResponse<Long> getMyProfileViewCount(Authentication authentication) {
        UUID keycloakId = UUID.fromString(authentication.getName());
        UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();
        long count = profileViewRepository.countByProfileOwnerId(internalUserId);
        return BaseResponse.<Long>builder()
                .status(HttpStatus.OK.value())
                .result(count)
                .build();
    }

    @GetMapping("/me/views/trends")
    public BaseResponse<List<Map<String, Object>>> getMyProfileViewTrends(Authentication authentication) {
        UUID keycloakId = UUID.fromString(authentication.getName());
        UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();
        
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Object[]> results = profileViewRepository.getDailyViewTrends(internalUserId, sevenDaysAgo);
        
        List<Map<String, Object>> trends = results.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", row[0].toString());
            map.put("count", row[1]);
            return map;
        }).collect(java.util.stream.Collectors.toList());

        return BaseResponse.<List<Map<String, Object>>>builder()
                .status(HttpStatus.OK.value())
                .result(trends)
                .build();
    }

    /**
     * Get posts authored by the logged-in user
     */
    @GetMapping("/me/posts")
    public BaseResponse<org.springframework.data.domain.Page<Post>> getMyPosts(Authentication authentication, org.springframework.data.domain.Pageable pageable) {
        UUID keycloakId = UUID.fromString(authentication.getName());
        UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();
        org.springframework.data.domain.Page<Post> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(internalUserId, pageable);
        return BaseResponse.<org.springframework.data.domain.Page<Post>>builder()
                .status(HttpStatus.OK.value())
                .result(posts)
                .build();
    }

    /**
     * Get posts authored by a specific internal User ID
     */
    @GetMapping("/{userId}/posts")
    public BaseResponse<org.springframework.data.domain.Page<Post>> getPostsByUser(@PathVariable UUID userId, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Post> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageable);
        return BaseResponse.<org.springframework.data.domain.Page<Post>>builder()
                .status(HttpStatus.OK.value())
                .result(posts)
                .build();
    }

    /**
     * Create or update logged-in user's profile
     */
    @PostMapping
    @PutMapping
    public BaseResponse<ProfileDTO> saveOrUpdateProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileDTO profileDTO
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        ProfileDTO profile = profileService.saveOrUpdate(userId, profileDTO);
        return BaseResponse.<ProfileDTO>builder()
                .status(HttpStatus.OK.value())
                .result(profile)
                .build();
    }

    /**
     * Public profile view (LinkedIn style)
     */
    @GetMapping("/{userId}")
    public BaseResponse<ProfileDTO> getProfileByUserId(
            @PathVariable UUID userId
    ) {
        ProfileDTO profile = profileService.getByUserId(userId);
        return BaseResponse.<ProfileDTO>builder()
                .status(HttpStatus.OK.value())
                .result(profile)
                .build();
    }

    @GetMapping("/search")
    public BaseResponse<List<ProfileDTO>> searchProfiles(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String headline,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder
    ) {
        List<ProfileDTO> profiles = profileService.searchProfiles(query, city, state, company, headline, sortBy, sortOrder);
        return BaseResponse.<List<ProfileDTO>>builder()
                .status(HttpStatus.OK.value())
                .result(profiles)
                .build();
    }

    @PostMapping("/cover-image")
    public ResponseEntity<BaseResponse<String>> updateCoverImage(
            Authentication authentication,
            @RequestParam("image") org.springframework.web.multipart.MultipartFile image) throws java.io.IOException {
        UUID userId = UUID.fromString(authentication.getName());
        String imageUrl = profileService.updateCoverImage(userId, image);
        return ResponseEntity.ok(BaseResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .result(imageUrl)
                .build());
    }

    @PostMapping("/experience")
    public BaseResponse<Experience> addExperience(Authentication authentication, @RequestBody Experience experience) {
        UUID userId = UUID.fromString(authentication.getName());
        Profile profile = profileRepo.findByUserId(userService.getUserByKeyCloakId(userId).getBody().getResult().getId());
        experience.setProfile(profile);
        Experience result = experienceRepository.save(experience);
        return BaseResponse.<Experience>builder()
                .status(HttpStatus.OK.value())
                .result(result)
                .build();
    }

    @PutMapping("/experience/{id}")
    public BaseResponse<Experience> updateExperience(Authentication authentication, @PathVariable UUID id, @RequestBody Experience experienceDetails) {
        UUID userId = UUID.fromString(authentication.getName());
        Profile profile = profileRepo.findByUserId(userService.getUserByKeyCloakId(userId).getBody().getResult().getId());
        Experience experience = experienceRepository.findById(id).orElseThrow(() -> new RuntimeException("Experience not found"));
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
        return BaseResponse.<Experience>builder()
                .status(HttpStatus.OK.value())
                .result(result)
                .build();
    }

    @DeleteMapping("/experience/{id}")
    public BaseResponse<Void> deleteExperience(Authentication authentication, @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        Profile profile = profileRepo.findByUserId(userService.getUserByKeyCloakId(userId).getBody().getResult().getId());
        Experience experience = experienceRepository.findById(id).orElseThrow(() -> new RuntimeException("Experience not found"));
        if (!experience.getProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        experienceRepository.delete(experience);
        return BaseResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Experience deleted")
                .build();
    }

    @PostMapping("/education")
    public BaseResponse<Education> addEducation(Authentication authentication, @RequestBody Education education) {
        UUID userId = UUID.fromString(authentication.getName());
        Profile profile = profileRepo.findByUserId(userService.getUserByKeyCloakId(userId).getBody().getResult().getId());
        education.setProfile(profile);
        Education result = educationRepository.save(education);
        return BaseResponse.<Education>builder()
                .status(HttpStatus.OK.value())
                .result(result)
                .build();
    }

    @PutMapping("/education/{id}")
    public BaseResponse<Education> updateEducation(Authentication authentication, @PathVariable UUID id, @RequestBody Education educationDetails) {
        UUID userId = UUID.fromString(authentication.getName());
        Profile profile = profileRepo.findByUserId(userService.getUserByKeyCloakId(userId).getBody().getResult().getId());
        Education education = educationRepository.findById(id).orElseThrow(() -> new RuntimeException("Education not found"));
        if (!education.getProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        education.setInstitution(educationDetails.getInstitution());
        education.setDegree(educationDetails.getDegree());
        education.setFieldOfStudy(educationDetails.getFieldOfStudy());
        education.setStartDate(educationDetails.getStartDate());
        education.setEndDate(educationDetails.getEndDate());
        Education result = educationRepository.save(education);
        return BaseResponse.<Education>builder()
                .status(HttpStatus.OK.value())
                .result(result)
                .build();
    }

    @DeleteMapping("/education/{id}")
    public BaseResponse<Void> deleteEducation(Authentication authentication, @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        Profile profile = profileRepo.findByUserId(userService.getUserByKeyCloakId(userId).getBody().getResult().getId());
        Education education = educationRepository.findById(id).orElseThrow(() -> new RuntimeException("Education not found"));
        if (!education.getProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        educationRepository.delete(education);
        return BaseResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Education deleted")
                .build();
    }

    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
}
