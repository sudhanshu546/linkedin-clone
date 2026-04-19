package com.org.linkedin.utility.client;

import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.PostEnrichmentDTO;
import com.org.linkedin.dto.user.TUserDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class UserServiceFallback implements UserService {

  @Override
  public ResponseEntity<ApiResponse<TUserDTO>> getUserByKeyCloakId(UUID keyCloakId) {
    return createFallbackResponse(keyCloakId);
  }

  @Override
  public ResponseEntity<ApiResponse<TUserDTO>> getUserById(UUID id) {
    return createFallbackResponse(id);
  }

  @Override
  public ResponseEntity<ApiResponse<com.org.linkedin.domain.enumeration.ReactionType>>
      getUserReaction(UUID postId) {
    return ResponseEntity.ok(ApiResponse.success("Fallback: User service unavailable", null));
  }

  @Override
  public ResponseEntity<ApiResponse<PostEnrichmentDTO>> getPostEnrichment(List<UUID> postIds) {
    return ResponseEntity.ok(
        ApiResponse.<PostEnrichmentDTO>builder()
            .status("partial")
            .message("User service unavailable. Returning empty enrichment data.")
            .data(
                PostEnrichmentDTO.builder()
                    .reactionCounts(new java.util.HashMap<>())
                    .commentCounts(new java.util.HashMap<>())
                    .userReactions(new java.util.HashMap<>())
                    .build())
            .build());
  }

  @Override
  public ResponseEntity<ApiResponse<java.util.List<TUserDTO>>> getUsersByIds(
      java.util.List<UUID> ids) {
    return ResponseEntity.ok(
        ApiResponse.<java.util.List<TUserDTO>>builder()
            .status("partial")
            .message("User service unavailable. Returning empty list.")
            .data(java.util.Collections.emptyList())
            .build());
  }

  private ResponseEntity<ApiResponse<TUserDTO>> createFallbackResponse(UUID id) {
    TUserDTO fallbackUser = new TUserDTO();
    fallbackUser.setId(id);
    fallbackUser.setFirstName("System");
    fallbackUser.setLastName("Fallback");
    fallbackUser.setEmail("unavailable@system.local");

    return ResponseEntity.ok(
        ApiResponse.<TUserDTO>builder()
            .status("partial")
            .message("User service is temporarily unavailable. Returning fallback data.")
            .data(fallbackUser)
            .build());
  }
}
