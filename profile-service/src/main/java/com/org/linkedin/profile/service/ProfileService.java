package com.org.linkedin.profile.service;

import com.org.linkedin.dto.ProfileDTO;
import com.org.linkedin.dto.ProfileSkillDTO;
import com.org.linkedin.dto.RecommendationDTO;
import com.org.linkedin.utility.service.AdvanceSearchCriteria;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface ProfileService {
  ProfileDTO getByUserId(UUID userId);

  ProfileDTO saveOrUpdate(UUID userId, ProfileDTO profileDTO);

  List<ProfileDTO> searchProfiles(
      String query,
      String city,
      String state,
      String company,
      String headline,
      String sortBy,
      String sortOrder);

  List<ProfileDTO> advancedSearch(AdvanceSearchCriteria criteria);

  String updateCoverImage(UUID userId, MultipartFile image)
      throws IOException;

  // Skills
  ProfileSkillDTO addSkill(UUID userId, String skillName, String category);

  void removeSkill(UUID userId, UUID profileSkillId);

  void endorseSkill(UUID endorserId, UUID profileSkillId);

  // Recommendations
  RecommendationDTO requestRecommendation(
      UUID requesterId, UUID authorId, String message);

  RecommendationDTO submitRecommendation(
      UUID authorId, UUID recommendationId, String content);

  void updateRecommendationStatus(UUID userId, UUID recommendationId, String status);

  // Recommendations Engine
  List<ProfileDTO> getPeopleYouMayKnow(UUID userId);
}
