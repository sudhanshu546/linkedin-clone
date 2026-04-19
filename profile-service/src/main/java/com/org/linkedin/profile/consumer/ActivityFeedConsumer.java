package com.org.linkedin.profile.consumer;

import static com.org.linkedin.utility.ProjectConstants.*;
import static com.org.linkedin.utility.ProjectConstants.GROUP_PROFILE_FEED;

import com.org.linkedin.dto.event.*;
import com.org.linkedin.profile.domain.Post;
import com.org.linkedin.profile.repo.PostRepository;
import com.org.linkedin.profile.service.ActivityFeedService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityFeedConsumer {

  private final ActivityFeedService activityFeedService;
  private final PostRepository postRepository;

  @KafkaListener(topics = TOPIC_USER_UPDATED, groupId = GROUP_PROFILE_FEED)
  @CacheEvict(value = "profiles", key = "#event.id")
  public void consumeUserUpdatedEvent(@Payload UserUpdatedEvent event) {
    log.info("Received UserUpdatedEvent for cache eviction and local sync: {}", event.getId());
    try {
      // If user profile image or name changed, we might want to update local Post cache in
      // profile-service
      List<Post> userPosts = postRepository.findByAuthorId(event.getId());
      if (!userPosts.isEmpty() && event.getProfileImageUrl() != null) {
        for (Post p : userPosts) {
          p.setUserProfileImageUrl(event.getProfileImageUrl());
        }
        postRepository.saveAll(userPosts);
        log.info(
            "Updated profile image for {} local posts of user {}", userPosts.size(), event.getId());
      }
    } catch (Exception e) {
      log.error("Failed to sync user update to local posts: {}", e.getMessage());
    }
  }

  @KafkaListener(topics = "${kafka.topics.post-created}", groupId = GROUP_PROFILE_FEED)
  public void consumePostCreatedEvent(@Payload PostCreatedEvent event) {
    log.info("!!! KAFKA RECEIVED !!! PostCreatedEvent: {}", event);
    try {
      // Save the full post data in profile-service local storage
      Post post =
          Post.builder()
              .postId(UUID.fromString(event.getPostId()))
              .authorId(UUID.fromString(event.getUserId()))
              .content(event.getContent())
              .userProfileImageUrl(event.getUserProfileImageUrl())
              .imageUrl(event.getImageUrl())
              .imageUrls(event.getImageUrls())
              .isPoll(event.isPoll())
              .createdDate(System.currentTimeMillis())
              .build();
      postRepository.save(post);
      log.info("Post saved to profile-service DB: {}", post.getPostId());

      activityFeedService.createFeedItem(event);
      log.info("Activity feed item created successfully for post: {}", event.getPostId());
    } catch (Exception e) {
      log.error("CRITICAL ERROR in PostCreatedEvent consumer: {}", e.getMessage(), e);
    }
  }

  @KafkaListener(topics = "${kafka.topics.post-reacted}", groupId = GROUP_PROFILE_FEED)
  public void consumePostReactedEvent(PostReactedEvent event) {
    log.info("Received PostReactedEvent: {}", event);
    activityFeedService.createFeedItem(event);
    updatePostCount(event.getPostId(), 1, 0);
  }

  @KafkaListener(topics = "${kafka.topics.comment-created}", groupId = GROUP_PROFILE_FEED)
  public void consumeCommentCreatedEvent(CommentCreatedEvent event) {
    log.info("Received CommentCreatedEvent: {}", event);
    activityFeedService.createFeedItem(event);
    updatePostCount(event.getPostId(), 0, 1);
  }

  @KafkaListener(
      topics = "${kafka.topics.post-unreacted:post-unreacted}",
      groupId = GROUP_PROFILE_FEED)
  public void handlePostUnreacted(PostUnreactedEvent event) {
    log.info("Received PostUnreactedEvent: {}", event);
    updatePostCount(event.getPostId(), -1, 0);
  }

  @KafkaListener(
      topics = "${kafka.topics.comment-deleted:comment-deleted}",
      groupId = GROUP_PROFILE_FEED)
  public void handleCommentDeleted(CommentDeletedEvent event) {
    log.info("Received CommentDeletedEvent: {}", event);
    updatePostCount(event.getPostId(), 0, -1);
  }

  @KafkaListener(topics = "${kafka.topics.post-deleted:post-deleted}", groupId = GROUP_PROFILE_FEED)
  @org.springframework.transaction.annotation.Transactional
  public void handlePostDeleted(PostDeletedEvent event) {
    log.info("Received PostDeletedEvent: {}", event);
    try {
      UUID postId = UUID.fromString(event.getPostId());
      postRepository.deleteByPostId(postId);
      ((com.org.linkedin.profile.repo.ActivityFeedItemRepository)
              activityFeedService.getActivityFeedItemRepository())
          .deleteByPostId(postId);
      log.info("Successfully deleted post and feed items for postId: {}", postId);
    } catch (Exception e) {
      log.error("Failed to process PostDeletedEvent in profile-service: {}", e.getMessage());
    }
  }

  private void updatePostCount(String postIdStr, int reactionDelta, int commentDelta) {
    try {
      java.util.UUID postId = java.util.UUID.fromString(postIdStr);
      postRepository
          .findById(postId)
          .ifPresent(
              post -> {
                post.setReactionCount(Math.max(0, post.getReactionCount() + reactionDelta));
                post.setCommentCount(Math.max(0, post.getCommentCount() + commentDelta));
                postRepository.save(post);
              });
    } catch (Exception e) {
      log.error("Failed to update post count in profile-service: {}", e.getMessage());
    }
  }

  @KafkaListener(topics = "${kafka.topics.connection-requested}", groupId = GROUP_PROFILE_FEED)
  public void consumeConnectionRequestedEvent(ConnectionRequestedEvent event) {
    log.info("Received ConnectionRequestedEvent: {}", event);
    activityFeedService.createFeedItem(event);
  }

  @KafkaListener(topics = "${kafka.topics.connection-accepted}", groupId = GROUP_PROFILE_FEED)
  public void consumeConnectionAcceptedEvent(ConnectionAcceptedEvent event) {
    log.info("Received ConnectionAcceptedEvent: {}", event);
    activityFeedService.createFeedItem(event);
  }
}
