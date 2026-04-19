package com.org.linkedin.profile.service;

import static com.org.linkedin.utility.ProjectConstants.DEFAULT_DESIGNATION;

import com.org.linkedin.domain.ActivityFeedItem;
import com.org.linkedin.dto.event.*;
import com.org.linkedin.profile.repo.ActivityFeedItemRepository;
import com.org.linkedin.utility.client.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for managing the user's activity feed. Implements a "Push-on-Write" (Fan-out)
 * strategy to ensure low latency for feed reads. Integrated with Redis for ultra-fast retrieval of
 * user timelines.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ActivityFeedService {

  private final ActivityFeedItemRepository activityFeedItemRepository;
  private final com.org.linkedin.profile.repo.ConnectionRepository connectionRepository;
  private final UserService userService;
  private final StringRedisTemplate redisTemplate;

  private static final String FEED_KEY_PREFIX = "feed:user:";
  private static final int MAX_FEED_SIZE = 500;

  /**
   * Distributes a feed item to all connections of the actor. Metadata is stored in PostgreSQL,
   * while Post IDs are pushed to Redis for speed.
   */
  @Async
  protected void fanOutFeedItem(
      UUID actorId,
      String actorName,
      String actorDesignation,
      String actorAvatar,
      String content,
      String type,
      String imageUrl,
      List<String> imageUrls,
      UUID postId,
      java.util.Map<String, String> metadata) {
    log.info("Starting background fan-out for actor {}. Type: {}", actorId, type);

    // 1. Save for the actor themselves
    saveFeedItem(
        actorId,
        actorId,
        actorName,
        actorDesignation,
        actorAvatar,
        content,
        type,
        imageUrl,
        imageUrls,
        postId,
        1.0,
        metadata);

    // 2. Push to all "Accepted" connections
    try {
      connectionRepository
          .findByRequesterIdAndStatus(
              actorId, com.org.linkedin.domain.enumeration.ConnectionStatus.ACCEPTED)
          .forEach(
              conn ->
                  saveFeedItem(
                      conn.getReceiverId(),
                      actorId,
                      actorName,
                      actorDesignation,
                      actorAvatar,
                      content,
                      type,
                      imageUrl,
                      imageUrls,
                      postId,
                      0.8,
                      metadata));

      connectionRepository
          .findByReceiverIdAndStatus(
              actorId, com.org.linkedin.domain.enumeration.ConnectionStatus.ACCEPTED)
          .forEach(
              conn ->
                  saveFeedItem(
                      conn.getRequesterId(),
                      actorId,
                      actorName,
                      actorDesignation,
                      actorAvatar,
                      content,
                      type,
                      imageUrl,
                      imageUrls,
                      postId,
                      0.8,
                      metadata));

      log.info("Completed background fan-out for post {} by actor {}", postId, actorId);
    } catch (Exception e) {
      log.error("Error during asynchronous feed fan-out: {}", e.getMessage());
    }
  }

  private void saveFeedItem(
      UUID userId,
      UUID actorId,
      String actorName,
      String actorDesignation,
      String actorAvatar,
      String content,
      String type,
      String imageUrl,
      List<String> imageUrls,
      UUID postId,
      Double priority,
      java.util.Map<String, String> metadata) {
    try {
      ActivityFeedItem item =
          ActivityFeedItem.builder()
              .userId(userId)
              .actorId(actorId)
              .actorName(actorName)
              .actorDesignation(actorDesignation)
              .actorAvatar(actorAvatar)
              .content(content)
              .type(type)
              .imageUrl(imageUrl)
              .imageUrls(imageUrls)
              .postId(postId)
              .timestamp(LocalDateTime.now())
              .priority(priority)
              .metadata(metadata)
              .build();

      item.setCreatedDate(System.currentTimeMillis());
      item.setIsDeleted(false);
      item.setIsEnabled(true);

      activityFeedItemRepository.save(item);

      // Push to Redis Timeline for O(1) retrieval
      if (postId != null) {
        String key = FEED_KEY_PREFIX + userId.toString();
        redisTemplate.opsForList().leftPush(key, item.getId().toString());
        redisTemplate.opsForList().trim(key, 0, MAX_FEED_SIZE - 1);
      }
    } catch (Exception e) {
      log.error("CRITICAL: Could not persist feed item for user {}: {}", userId, e.getMessage());
    }
  }

  public void createFeedItem(PostCreatedEvent event) {
    java.util.Map<String, String> metadata = new java.util.HashMap<>();
    if (event.isPoll()) {
      metadata.put("isPoll", "true");
      metadata.put("pollQuestion", event.getPollQuestion());
      metadata.put("pollOptions", String.join(",", event.getPollOptions()));
    }

    fanOutFeedItem(
        UUID.fromString(event.getUserId()),
        event.getUserName(),
        event.getUserDesignation(),
        event.getUserProfileImageUrl(),
        event.getContent(),
        event.isPoll() ? "POLL_CREATED" : "POST_CREATED",
        event.getImageUrl(),
        event.getImageUrls(),
        UUID.fromString(event.getPostId()),
        metadata);
  }

  public void createFeedItem(ConnectionRequestedEvent event) {
    var actorResponse = userService.getUserById(event.getSenderId());
    if (actorResponse == null || actorResponse.getBody() == null) return;

    var actor = actorResponse.getBody().getData();
    String actorName =
        actor.getFirstName() + (actor.getLastName() != null ? " " + actor.getLastName() : "");

    saveFeedItem(
        event.getReceiverId(),
        event.getSenderId(),
        actorName,
        DEFAULT_DESIGNATION,
        actor.getProfileImageUrl(),
        "sent you a connection request",
        "CONNECTION_REQUESTED",
        null,
        null,
        null,
        0.5,
        null);
  }

  public void createFeedItem(ConnectionAcceptedEvent event) {
    var receiverBody = userService.getUserById(event.getReceiverId()).getBody();
    var requesterBody = userService.getUserById(event.getRequesterId()).getBody();
    if (receiverBody == null || requesterBody == null) return;

    var receiver = receiverBody.getData();
    var requester = requesterBody.getData();

    saveFeedItem(
        event.getRequesterId(),
        event.getReceiverId(),
        receiver.getFirstName() + " " + receiver.getLastName(),
        DEFAULT_DESIGNATION,
        receiver.getProfileImageUrl(),
        "accepted your connection request",
        "CONNECTION_ACCEPTED",
        null,
        null,
        null,
        0.9,
        null);
    saveFeedItem(
        event.getReceiverId(),
        event.getRequesterId(),
        requester.getFirstName() + " " + requester.getLastName(),
        DEFAULT_DESIGNATION,
        requester.getProfileImageUrl(),
        "is now connected with you",
        "CONNECTION_ACCEPTED",
        null,
        null,
        null,
        0.9,
        null);
  }

  public void createFeedItem(PostReactedEvent event) {
    if (event.getUserId().equals(event.getPostAuthorId())) return;

    // We need to fetch the actor's avatar if not in event (it's not in the DTO currently)
    String actorAvatar = null;
    try {
      var actorRes = userService.getUserById(UUID.fromString(event.getUserId()));
      if (actorRes != null && actorRes.getBody() != null)
        actorAvatar = actorRes.getBody().getData().getProfileImageUrl();
    } catch (Exception e) {
    }

    saveFeedItem(
        UUID.fromString(event.getPostAuthorId()),
        UUID.fromString(event.getUserId()),
        event.getUserName(),
        event.getUserDesignation(),
        actorAvatar,
        "reacted to your post",
        "POST_REACTED",
        null,
        null,
        UUID.fromString(event.getPostId()),
        0.7,
        java.util.Map.of("reactionType", event.getReactionType().toString()));
  }

  public void createFeedItem(CommentCreatedEvent event) {
    fanOutFeedItem(
        UUID.fromString(event.getUserId()),
        event.getUserName(),
        event.getUserDesignation(),
        event.getUserProfileImageUrl(),
        "commented on a post: " + event.getContent(),
        "COMMENT_CREATED",
        null,
        null,
        UUID.fromString(event.getPostId()),
        java.util.Map.of("commentId", event.getCommentId()));
  }

  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<ActivityFeedItem> getFeedForUser(
      UUID userId, org.springframework.data.domain.Pageable pageable) {
    return activityFeedItemRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
  }

  public org.springframework.data.domain.Page<ActivityFeedItem> getFeedForUserPaginated(
      UUID userId, org.springframework.data.domain.Pageable pageable) {
    return activityFeedItemRepository.findByUserIdOrderByPriorityDescTimestampDesc(
        userId, pageable);
  }

  public com.org.linkedin.profile.repo.ActivityFeedItemRepository getActivityFeedItemRepository() {
    return activityFeedItemRepository;
  }
}
