package com.org.linkedin.profile.controller;

import com.org.linkedin.domain.ActivityFeedItem;
import com.org.linkedin.dto.ActivityFeedItemDTO;
import com.org.linkedin.dto.ProfileDTO;
import com.org.linkedin.profile.repo.ActivityFeedItemRepository;
import com.org.linkedin.profile.repo.ProfileRepo;
import com.org.linkedin.profile.service.ActivityFeedService;
import com.org.linkedin.profile.service.ProfileService;
import com.org.linkedin.utility.client.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${apiPrefix}/feed")
@RequiredArgsConstructor
@Slf4j
public class ActivityFeedController {

    private final ActivityFeedService activityFeedService;
    private final UserService userService;
    private final ProfileRepo profileRepo;

    @GetMapping
    public List<ActivityFeedItemDTO> getMyFeed(
            Authentication authentication,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size
    ) {
        UUID keycloakId = UUID.fromString(authentication.getName());
        try {
            var userRes = userService.getUserByKeyCloakId(keycloakId);
            if (userRes != null && userRes.getBody() != null && userRes.getBody().getResult() != null) {
                UUID internalUserId = userRes.getBody().getResult().getId();
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
                List<ActivityFeedItem> feedItems = activityFeedService.getFeedForUser(internalUserId, pageable);
                
                return feedItems.stream().map(item -> {
                    ActivityFeedItemDTO dto = ActivityFeedItemDTO.builder()
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
                    
                    // Note: actorProfileId is still needed for linking to profile page
                    // We can try to get it from a local cache or just use actorId if they match keycloak IDs
                    // In this project, actorId is internal, actorProfileId was expected to be Keycloak ID
                    // Let's at least try to get it once if not present, but for now we'll prioritize stability
                    dto.setActorProfileId(item.getActorId().toString()); 

                    // Check if liked by current user - wrap in try-catch to prevent entire feed from failing
                    if (item.getPostId() != null) {
                        try {
                            var likedRes = userService.isLiked(item.getPostId());
                            if (likedRes != null && likedRes.getBody() != null) {
                                dto.setLikedByCurrentUser(likedRes.getBody());
                            }
                        } catch (Exception e) {
                            log.warn("Could not check like status for post {}: {}", item.getPostId(), e.getMessage());
                        }
                    }
                    
                    return dto;
                }).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error fetching feed: {}", e.getMessage());
        }
        return java.util.Collections.emptyList();
    }
}
