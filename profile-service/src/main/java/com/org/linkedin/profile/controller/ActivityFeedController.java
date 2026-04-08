package com.org.linkedin.profile.controller;

import com.org.linkedin.domain.ActivityFeedItem;
import com.org.linkedin.dto.ActivityFeedItemDTO;
import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.profile.repo.ProfileRepo;
import com.org.linkedin.profile.service.ActivityFeedService;
import com.org.linkedin.utility.client.UserService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${apiPrefix}/feed")
@RequiredArgsConstructor
@Slf4j
public class ActivityFeedController {

  private final ActivityFeedService activityFeedService;
  private final UserService userService;
  private final ProfileRepo profileRepo;

  @GetMapping
  public ResponseEntity<ApiResponse<List<ActivityFeedItemDTO>>> getMyFeed(
      Authentication authentication,
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    try {
      var userRes = userService.getUserByKeyCloakId(keycloakId);
      if (userRes != null && userRes.getBody() != null && userRes.getBody().getData() != null) {
        UUID internalUserId = userRes.getBody().getData().getId();
        org.springframework.data.domain.Pageable pageable =
            org.springframework.data.domain.PageRequest.of(page, size);
        Page<ActivityFeedItem> feedPage =
            activityFeedService.getFeedForUserPaginated(internalUserId, pageable);

        List<UUID> postIds = feedPage.getContent().stream()
            .map(ActivityFeedItem::getPostId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());

        com.org.linkedin.dto.PostEnrichmentDTO enrichment = null;
        if (!postIds.isEmpty()) {
          try {
            var enrichmentRes = userService.getPostEnrichment(postIds);
            if (enrichmentRes != null && enrichmentRes.getBody() != null) {
              enrichment = enrichmentRes.getBody().getData();
            }
          } catch (Exception e) {
            log.warn("Failed to fetch feed enrichment: {}", e.getMessage());
          }
        }

        final com.org.linkedin.dto.PostEnrichmentDTO finalEnrichment = enrichment;

        List<ActivityFeedItemDTO> dtoList =
            feedPage.getContent().stream()
                .map(
                    item -> {
                      ActivityFeedItemDTO dto =
                          ActivityFeedItemDTO.builder()
                              .id(item.getId())
                              .userId(item.getUserId())
                              .actorId(item.getActorId())
                              .actorName(item.getActorName())
                              .actorDesignation(item.getActorDesignation())
                              .postId(item.getPostId())
                              .content(item.getContent())
                              .type(item.getType())
                              .imageUrl(item.getImageUrl())
                              .imageUrls(item.getImageUrls())
                              .timestamp(item.getTimestamp())
                              .build();

                      dto.setActorProfileId(item.getActorId().toString());

                      if (item.getPostId() != null && finalEnrichment != null) {
                        dto.setReactionCount(finalEnrichment.getReactionCounts().getOrDefault(item.getPostId(), 0L));
                        dto.setCommentCount(finalEnrichment.getCommentCounts().getOrDefault(item.getPostId(), 0L));
                        var reaction = finalEnrichment.getUserReactions().get(item.getPostId());
                        if (reaction != null) {
                          dto.setUserReaction(reaction.toString());
                          dto.setLikedByCurrentUser(true);
                        }
                      }

                      return dto;
                    })
                .collect(Collectors.toList());

        return ResponseEntity.ok(
            ApiResponse.success(
                "Feed fetched successfully",
                dtoList,
                feedPage.getNumber(),
                feedPage.getSize(),
                feedPage.getTotalElements()));
      }
    } catch (Exception e) {
      log.error("Error fetching feed: {}", e.getMessage());
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }
}
