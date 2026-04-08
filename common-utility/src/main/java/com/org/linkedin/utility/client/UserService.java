package com.org.linkedin.utility.client;

import com.org.linkedin.domain.enumeration.ReactionType;
import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.user.TUserDTO;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/us", fallback = UserServiceFallback.class)
public interface UserService {

  @GetMapping("/user/user/{id}")
  public ResponseEntity<ApiResponse<TUserDTO>> getUserByKeyCloakId(
      @PathVariable("id") UUID keyCloakId);

  @GetMapping("/user/{id}")
  public ResponseEntity<ApiResponse<TUserDTO>> getUserById(@PathVariable("id") UUID id);

  @GetMapping("/posts/{postId}/reaction")
  public ResponseEntity<ApiResponse<ReactionType>> getUserReaction(
      @PathVariable("postId") UUID postId);

  @org.springframework.web.bind.annotation.PostMapping("/posts/enrichment")
  public ResponseEntity<ApiResponse<com.org.linkedin.dto.PostEnrichmentDTO>> getPostEnrichment(
      @org.springframework.web.bind.annotation.RequestBody java.util.List<UUID> postIds);
}
