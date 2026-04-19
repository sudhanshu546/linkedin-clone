package com.org.linkedin.profile.controller;

import com.org.linkedin.domain.ActivityFeedItem;
import com.org.linkedin.dto.ActivityFeedItemDTO;
import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.PostEnrichmentDTO;
import com.org.linkedin.profile.repo.ProfileRepo;
import com.org.linkedin.profile.service.ActivityFeedService;
import com.org.linkedin.utility.client.UserService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for the LinkedIn Activity Feed. Implements complex feed generation including fan-out
 * retrieval and cross-service bulk enrichment for high performance. Now optimized with Redis
 * timeline caching.
 */
@RestController
@RequestMapping("${apiPrefix}/feed")
@RequiredArgsConstructor
@Slf4j
public class ActivityFeedController {

  private final ActivityFeedService activityFeedService;
  private final UserService userService;
  private final ProfileRepo profileRepo;
  private final StringRedisTemplate redisTemplate;

  private static final String FEED_KEY_PREFIX = "feed:user:";

  /**
   * Retrieves the personalized activity feed for the authenticated user. This method uses a hybrid
   * Push/Pull strategy: 1. Attempt to fetch pre-computed timeline from Redis (Push model). 2. If
   * Redis is empty, fallback to the fan-out table in PostgreSQL (Pull model). 3. Perform a single
   * bulk enrichment call to the User Service for interactions.
   *
   * @param authentication The authenticated user security context from Keycloak.
   * @param page Zero-based page index for pagination.
   * @param size Number of feed items per page for pagination.
   * @return A ResponseEntity containing an ApiResponse with a paginated list of enriched
   *     ActivityFeedItemDTOs.
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<ActivityFeedItemDTO>>> getMyFeed(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    UUID keycloakId = UUID.fromString(authentication.getName());
    try {
      var userRes = userService.getUserByKeyCloakId(keycloakId);
      if (userRes != null && userRes.getBody() != null && userRes.getBody().getData() != null) {
        UUID internalUserId = userRes.getBody().getData().getId();

        List<ActivityFeedItem> feedItems;
        long totalElements;

        // Try Redis First
        String key = FEED_KEY_PREFIX + internalUserId.toString();
        long start = (long) page * size;
        long end = start + size - 1;
        List<String> ids = redisTemplate.opsForList().range(key, start, end);

        if (ids != null && !ids.isEmpty()) {
          log.debug("Cache Hit: Fetching feed from Redis for user {}", internalUserId);
          List<UUID> uuidIds = ids.stream().map(UUID::fromString).collect(Collectors.toList());
          List<ActivityFeedItem> unsortedItems =
              activityFeedService.getActivityFeedItemRepository().findAllById(uuidIds);

          // Map for O(1) lookup to preserve order
          java.util.Map<UUID, ActivityFeedItem> itemMap =
              unsortedItems.stream()
                  .collect(Collectors.toMap(ActivityFeedItem::getId, item -> item));

          feedItems =
              uuidIds.stream()
                  .map(itemMap::get)
                  .filter(java.util.Objects::nonNull)
                  .collect(Collectors.toList());

          Long total = redisTemplate.opsForList().size(key);
          totalElements = total != null ? total : (long) feedItems.size();
        } else {
          log.debug("Cache Miss: Falling back to SQL for user {}", internalUserId);
          Pageable pageable = PageRequest.of(page, size);
          Page<ActivityFeedItem> feedPage =
              activityFeedService.getFeedForUserPaginated(internalUserId, pageable);
          feedItems = feedPage.getContent();
          totalElements = feedPage.getTotalElements();
        }

        List<UUID> postIds =
            feedItems.stream()
                .map(ActivityFeedItem::getPostId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        // Performance Optimization: Bulk Enrichment trip to User Service
        PostEnrichmentDTO enrichment = null;
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

        final PostEnrichmentDTO finalEnrichment = enrichment;

        // Map domain entities to DTOs while merging statistics from enrichment
        List<ActivityFeedItemDTO> dtoList =
            feedItems.stream()
                .map(
                    item -> {
                      ActivityFeedItemDTO dto =
                          ActivityFeedItemDTO.builder()
                              .id(item.getId())
                              .userId(item.getUserId())
                              .actorId(item.getActorId())
                              .actorName(item.getActorName())
                              .actorDesignation(item.getActorDesignation())
                              .actorAvatar(item.getActorAvatar())
                              .postId(item.getPostId())
                              .content(item.getContent())
                              .type(item.getType())
                              .imageUrl(item.getImageUrl())
                              .imageUrls(item.getImageUrls())
                              .timestamp(item.getTimestamp())
                              .build();

                      dto.setActorProfileId(item.getActorId().toString());

                      if (item.getPostId() != null && finalEnrichment != null) {
                        dto.setReactionCount(
                            finalEnrichment.getReactionCounts().getOrDefault(item.getPostId(), 0L));
                        dto.setCommentCount(
                            finalEnrichment.getCommentCounts().getOrDefault(item.getPostId(), 0L));

                        // Handle Reaction
                        var reaction = finalEnrichment.getUserReactions().get(item.getPostId());
                        if (reaction != null) {
                          dto.setUserReaction(reaction.toString());
                          dto.setLikedByCurrentUser(true);
                        }

                        // Handle Polls
                        boolean isPollType =
                            "POLL_CREATED".equals(item.getType()) || "POLL".equals(item.getType());
                        boolean hasPollData =
                            finalEnrichment.getPollOptions() != null
                                && finalEnrichment.getPollOptions().containsKey(item.getPostId());

                        dto.setPoll(isPollType || hasPollData);

                        if (dto.isPoll()) {
                          dto.setPollQuestion(item.getContent());

                          // Priority 1: Enrichment data (most accurate vote counts)
                          if (hasPollData) {
                            dto.setPollOptions(
                                finalEnrichment.getPollOptions().get(item.getPostId()));
                            if (finalEnrichment.getHasVoted() != null) {
                              dto.setHasVoted(
                                  Boolean.TRUE.equals(
                                      finalEnrichment.getHasVoted().get(item.getPostId())));
                            }
                            if (finalEnrichment.getSelectedOptionIds() != null) {
                              dto.setSelectedOptionId(
                                  finalEnrichment.getSelectedOptionIds().get(item.getPostId()));
                            }
                          }
                          // Priority 2: Metadata fallback (if enrichment failed)
                          else if (item.getMetadata() != null
                              && item.getMetadata().containsKey("pollOptions")) {
                            String[] options = item.getMetadata().get("pollOptions").split(",");
                            dto.setPollOptions(
                                java.util.Arrays.stream(options)
                                    .map(
                                        opt ->
                                            com.org.linkedin.dto.poll.PollOptionDTO.builder()
                                                .text(opt)
                                                .voteCount(0L)
                                                .build())
                                    .collect(Collectors.toList()));
                          }
                        }
                      }

                      return dto;
                    })
                .collect(Collectors.toList());

        return ResponseEntity.ok(
            ApiResponse.success("Feed fetched successfully", dtoList, page, size, totalElements));
      }
    } catch (Exception e) {
      log.error("Error fetching feed: {}", e.getMessage(), e);
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }
}
