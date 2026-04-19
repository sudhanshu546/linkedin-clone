package com.org.linkedin.utility.client;

import com.org.linkedin.domain.enumeration.ReactionType;
import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.PostEnrichmentDTO;
import com.org.linkedin.dto.user.TUserDTO;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", path = "/us", fallback = UserServiceFallback.class)
public interface UserService {

  @GetMapping("/user/{id}")
  public ResponseEntity<ApiResponse<TUserDTO>> getUserByKeyCloakId(
      @PathVariable("id") UUID keyCloakId);

  @GetMapping("/user/{id}")
  public ResponseEntity<ApiResponse<TUserDTO>> getUserById(@PathVariable("id") UUID id);

  @GetMapping("/posts/{postId}/reaction")
  public ResponseEntity<ApiResponse<ReactionType>> getUserReaction(
      @PathVariable("postId") UUID postId);

  @PostMapping("/posts/enrichment")
  public ResponseEntity<ApiResponse<PostEnrichmentDTO>> getPostEnrichment(
      @RequestBody java.util.List<UUID> postIds);

  @GetMapping("/user/bulk")
  public ResponseEntity<ApiResponse<java.util.List<TUserDTO>>> getUsersByIds(
      @RequestParam("ids") java.util.List<UUID> ids);
}
