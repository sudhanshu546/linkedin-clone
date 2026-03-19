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

import com.org.linkedin.dto.BasePageResponse;
import com.org.linkedin.dto.BaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("${apiPrefix}/feed")
@RequiredArgsConstructor
@Slf4j
public class ActivityFeedController {

    private final ActivityFeedService activityFeedService;
    private final UserService userService;
    private final ProfileRepo profileRepo;

    @GetMapping
    public ResponseEntity<BasePageResponse<List<ActivityFeedItemDTO>>> getMyFeed(
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
                Page<ActivityFeedItem> feedPage = activityFeedService.getFeedForUserPaginated(internalUserId, pageable);
                
                List<ActivityFeedItemDTO> dtoList = feedPage.getContent().stream().map(item -> {
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
                    
                    dto.setActorProfileId(item.getActorId().toString()); 

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

                return ResponseEntity.ok(BasePageResponse.<List<ActivityFeedItemDTO>>builder()
                        .status(HttpStatus.OK.value())
                        .message("Feed fetched successfully")
                        .result(dtoList)
                        .pageNumber(feedPage.getNumber())
                        .pageSize(feedPage.getSize())
                        .totalRecords(feedPage.getTotalElements())
                        .build());
            }
        } catch (Exception e) {
            log.error("Error fetching feed: {}", e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
