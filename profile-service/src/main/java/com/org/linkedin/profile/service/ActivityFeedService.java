package com.org.linkedin.profile.service;

import static com.org.linkedin.utility.ProjectConstants.DEFAULT_DESIGNATION;

import com.org.linkedin.domain.ActivityFeedItem;
import com.org.linkedin.dto.event.CommentCreatedEvent;
import com.org.linkedin.dto.event.ConnectionAcceptedEvent;
import com.org.linkedin.dto.event.ConnectionRequestedEvent;
import com.org.linkedin.dto.event.PostCreatedEvent;
import com.org.linkedin.dto.event.PostReactedEvent;
import com.org.linkedin.profile.repo.ActivityFeedItemRepository;
import com.org.linkedin.utility.client.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for managing the user's activity feed.
 * Implements a "Push-on-Write" (Fan-out) strategy to ensure low latency for feed reads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ActivityFeedService {

  private final ActivityFeedItemRepository activityFeedItemRepository;
  private final com.org.linkedin.profile.repo.ConnectionRepository connectionRepository;
  private final UserService userService;

  /**
   * Distributes a feed item to all connections of the actor.
   * This operation is performed asynchronously to avoid blocking the event consumer.
   *
   * @param actorId The UUID of the user performing the action.
   * @param actorName Name of the user for display in the feed.
   * @param actorDesignation Professional designation of the user.
   * @param content Content of the post or activity.
   * @param type Type of activity (e.g., POST_CREATED, POLL_CREATED).
   * @param imageUrl Main image URL for the post.
   * @param imageUrls List of all image URLs associated with the post.
   * @param postId UUID of the original post.
   * @param metadata Additional context for the feed item.
   */
  @Async
  protected void fanOutFeedItem(
      UUID actorId,
      String actorName,
      String actorDesignation,
      String content,
      String type,
      String imageUrl,
      List<String> imageUrls,
      UUID postId,
      java.util.Map<String, String> metadata) {
    log.info("Starting background fan-out for actor {}. Type: {}", actorId, type);

    // 1. Save for the actor themselves (so they see it in their own feed)
    saveFeedItem(
        actorId,
        actorId,
        actorName,
        actorDesignation,
        content,
        type,
        imageUrl,
        imageUrls,
        postId,
        1.0,
        metadata);

    // 2. Push to all "Accepted" connections
    try {
      // Connections where actor was the requester
      connectionRepository
          .findByRequesterIdAndStatus(
              actorId, com.org.linkedin.domain.enumeration.ConnectionStatus.ACCEPTED)
          .forEach(
              conn -> {
                saveFeedItem(
                    conn.getReceiverId(),
                    actorId,
                    actorName,
                    actorDesignation,
                    content,
                    type,
                    imageUrl,
                    imageUrls,
                    postId,
                    0.8, 
                    metadata);
              });

      // Connections where actor was the receiver
      connectionRepository
          .findByReceiverIdAndStatus(
              actorId, com.org.linkedin.domain.enumeration.ConnectionStatus.ACCEPTED)
          .forEach(
              conn -> {
                saveFeedItem(
                    conn.getRequesterId(),
                    actorId,
                    actorName,
                    actorDesignation,
                    content,
                    type,
                    imageUrl,
                    imageUrls,
                    postId,
                    0.8,
                    metadata);
              });
      
      log.info("Completed background fan-out for post {} by actor {}", postId, actorId);
    } catch (Exception e) {
      log.error("Error during asynchronous feed fan-out: {}", e.getMessage());
    }
  }

  /**
   * Internal helper to persist a feed item for a specific recipient.
   *
   * @param userId The recipient of the feed item.
   * @param actorId The person who triggered the feed item.
   * @param priority Ranking weight for the feed algorithm.
   */
  private void saveFeedItem(
      UUID userId,
      UUID actorId,
      String actorName,
      String actorDesignation,
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
              .content(content)
              .type(type)
              .imageUrl(imageUrl)
              .imageUrls(imageUrls)
              .postId(postId)
              .timestamp(LocalDateTime.now())
              .priority(priority)
              .metadata(metadata)
              .build();

      // Ensure auditing fields are set manually if JPA auditing is still initializing
      item.setCreatedDate(System.currentTimeMillis());
      item.setIsDeleted(false);
      item.setIsEnabled(true);

      activityFeedItemRepository.save(item);
    } catch (Exception e) {
      log.error("CRITICAL: Could not persist feed item for user {}: {}", userId, e.getMessage());
    }
  }

  /**
   * Entry point for PostCreatedEvent. Triggers the fan-out process.
   * @param event The event consumed from Kafka.
   */
  public void createFeedItem(PostCreatedEvent event) {
    log.debug("Processing PostCreatedEvent for post: {}", event.getPostId());
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
        event.getContent(),
        event.isPoll() ? "POLL_CREATED" : "POST_CREATED",
        event.getImageUrl(),
        event.getImageUrls(),
        UUID.fromString(event.getPostId()),
        metadata);
  }

  /**
   * Entry point for ConnectionRequestedEvent.
   * Directly saves a feed item for the receiver.
   */
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
        "sent you a connection request",
        "CONNECTION_REQUESTED",
        null,
        null,
        null,
        0.5,
        null);
  }

  /**
   * Entry point for ConnectionAcceptedEvent.
   * Updates feeds for both participants.
   */
  public void createFeedItem(ConnectionAcceptedEvent event) {
    var receiverBody = userService.getUserById(event.getReceiverId()).getBody();
    var requesterBody = userService.getUserById(event.getRequesterId()).getBody();
    
    if (receiverBody == null || requesterBody == null) return;

    var receiver = receiverBody.getData();
    String receiverName = receiver.getFirstName() + " " + (receiver.getLastName() != null ? receiver.getLastName() : "");

    var requester = requesterBody.getData();
    String requesterName = requester.getFirstName() + " " + (requester.getLastName() != null ? requester.getLastName() : "");

    saveFeedItem(
        event.getRequesterId(),
        event.getReceiverId(),
        receiverName,
        DEFAULT_DESIGNATION,
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
        requesterName,
        DEFAULT_DESIGNATION,
        "is now connected with you",
        "CONNECTION_ACCEPTED",
        null,
        null,
        null,
        0.9,
        null);
  }

  /**
   * Entry point for PostReactedEvent.
   * Notifies the post author about the reaction.
   */
  public void createFeedItem(PostReactedEvent event) {
    if (event.getUserId().equals(event.getPostAuthorId())) return;

    saveFeedItem(
        UUID.fromString(event.getPostAuthorId()),
        UUID.fromString(event.getUserId()),
        event.getUserName(),
        event.getUserDesignation(),
        "reacted to your post",
        "POST_REACTED",
        null,
        null,
        UUID.fromString(event.getPostId()),
        0.7,
        java.util.Map.of("reactionType", event.getReactionType().toString()));
  }

  /**
   * Entry point for CommentCreatedEvent.
   * Fans out the comment activity to connections of the commenter.
   */
  public void createFeedItem(CommentCreatedEvent event) {
    fanOutFeedItem(
        UUID.fromString(event.getUserId()),
        event.getUserName(),
        event.getUserDesignation(),
        "commented on a post: " + event.getContent(),
        "COMMENT_CREATED",
        null,
        null,
        UUID.fromString(event.getPostId()),
        java.util.Map.of("commentId", event.getCommentId()));
  }

  /**
   * Retrieves the feed for a specific user ordered by timestamp.
   */
  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<ActivityFeedItem> getFeedForUser(
      UUID userId, org.springframework.data.domain.Pageable pageable) {
    return activityFeedItemRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
  }

  /**
   * Retrieves the feed for a specific user using an advanced priority-based ranking.
   */
  public org.springframework.data.domain.Page<ActivityFeedItem> getFeedForUserPaginated(
      UUID userId, org.springframework.data.domain.Pageable pageable) {
    return activityFeedItemRepository.findByUserIdOrderByPriorityDescTimestampDesc(
        userId, pageable);
  }
}
