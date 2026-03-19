package com.org.linkedin.profile.service;

import com.org.linkedin.domain.ActivityFeedItem;
import com.org.linkedin.dto.event.CommentCreatedEvent;
import com.org.linkedin.dto.event.ConnectionAcceptedEvent;
import com.org.linkedin.dto.event.ConnectionRequestedEvent;
import com.org.linkedin.dto.event.PostCreatedEvent;
import com.org.linkedin.dto.event.PostLikedEvent;
import com.org.linkedin.profile.repo.ActivityFeedItemRepository;
import com.org.linkedin.utility.client.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ActivityFeedService {

    private final ActivityFeedItemRepository activityFeedItemRepository;
    private final com.org.linkedin.profile.repo.ConnectionRepository connectionRepository;
    private final UserService userService;

    private void fanOutFeedItem(UUID actorId, String actorName, String actorDesignation, String content, String type, String imageUrl, List<String> imageUrls, UUID postId) {
        log.info("Starting fan-out for actor {}. Type: {}, PostId: {}", actorId, type, postId);
        
        // Save for the actor
        saveFeedItem(actorId, actorId, actorName, actorDesignation, content, type, imageUrl, imageUrls, postId);

        // Save for all connections
        try {
            connectionRepository.findByRequesterIdAndStatus(actorId, com.org.linkedin.domain.enumeration.ConnectionStatus.ACCEPTED)
                    .forEach(conn -> {
                        log.info("Fanning out to connection (receiver): {}", conn.getReceiverId());
                        saveFeedItem(conn.getReceiverId(), actorId, actorName, actorDesignation, content, type, imageUrl, imageUrls, postId);
                    });

            connectionRepository.findByReceiverIdAndStatus(actorId, com.org.linkedin.domain.enumeration.ConnectionStatus.ACCEPTED)
                    .forEach(conn -> {
                        log.info("Fanning out to connection (requester): {}", conn.getRequesterId());
                        saveFeedItem(conn.getRequesterId(), actorId, actorName, actorDesignation, content, type, imageUrl, imageUrls, postId);
                    });
        } catch (Exception e) {
            log.error("Error during feed fan-out: {}", e.getMessage());
        }
    }

    private void saveFeedItem(UUID userId, UUID actorId, String actorName, String actorDesignation, String content, String type, String imageUrl, List<String> imageUrls, UUID postId) {
        try {
            ActivityFeedItem item = ActivityFeedItem.builder()
                    .userId(userId)
                    .actorId(actorId)
                    .actorName(actorName)
                    .actorDesignation(actorDesignation)
                    .content(content)
                    .type(type)
                    .imageUrl(imageUrl)
                    .imageUrls(imageUrls)
                    .postId(postId)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            item.setCreatedAt(System.currentTimeMillis());
            item.setIsDeleted(false);
            item.setIsEnabled(true);
            
            activityFeedItemRepository.saveAndFlush(item);
            log.info("SUCCESS: Saved feed item for user {} (actor: {})", userId, actorId);
        } catch (Exception e) {
            log.error("FAILURE: Could not save feed item: {}", e.getMessage());
        }
    }

    public void createFeedItem(PostCreatedEvent event) {
        log.info("Processing PostCreatedEvent for post: {}", event.getPostId());
        // For posts, the 'content' string in ActivityFeedItem will just be the post text
        fanOutFeedItem(UUID.fromString(event.getUserId()), event.getUserName(), event.getUserDesignation(), event.getContent(), "POST_CREATED", event.getImageUrl(), event.getImageUrls(), UUID.fromString(event.getPostId()));
    }

    public void createFeedItem(PostLikedEvent event) {
        // We don't want to create a new feed item for likes
        // Likes are handled via post stats in the existing post card
        log.info("PostLikedEvent received, not creating feed item.");
    }

    public void createFeedItem(CommentCreatedEvent event) {
        // We don't want to create a new feed item for comments
        // Comments are handled via comment section in the existing post card
        log.info("CommentCreatedEvent received, not creating feed item.");
    }

    public void createFeedItem(ConnectionRequestedEvent event) {
        var actor = userService.getUserByInternalId(event.getSenderId()).getBody().getResult();
        String actorName = actor.getFirstName() + (actor.getLastName() != null ? " " + actor.getLastName() : "");
        saveFeedItem(event.getReceiverId(), event.getSenderId(), actorName, "LinkedIn Member", "sent you a connection request", "CONNECTION_REQUESTED", null, null, null);
    }

    public void createFeedItem(ConnectionAcceptedEvent event) {
        var receiver = userService.getUserByInternalId(event.getReceiverId()).getBody().getResult();
        String receiverName = receiver.getFirstName() + (receiver.getLastName() != null ? " " + receiver.getLastName() : "");
        
        var requester = userService.getUserByInternalId(event.getRequesterId()).getBody().getResult();
        String requesterName = requester.getFirstName() + (requester.getLastName() != null ? " " + requester.getLastName() : "");

        saveFeedItem(event.getRequesterId(), event.getReceiverId(), receiverName, "LinkedIn Member", "accepted your connection request", "CONNECTION_ACCEPTED", null, null, null);
        saveFeedItem(event.getReceiverId(), event.getRequesterId(), requesterName, "LinkedIn Member", "is now connected with you", "CONNECTION_ACCEPTED", null, null, null);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ActivityFeedItem> getFeedForUser(UUID userId, org.springframework.data.domain.Pageable pageable) {
        return activityFeedItemRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    public org.springframework.data.domain.Page<ActivityFeedItem> getFeedForUserPaginated(UUID userId, org.springframework.data.domain.Pageable pageable) {
        return activityFeedItemRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }
}
