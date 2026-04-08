package com.org.linkedin.profile.consumer;

import static com.org.linkedin.utility.ProjectConstants.GROUP_PROFILE_FEED;

import com.org.linkedin.dto.event.*;
import com.org.linkedin.profile.domain.Post;
import com.org.linkedin.profile.repo.PostRepository;
import com.org.linkedin.profile.service.ActivityFeedService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityFeedConsumer {

  private final ActivityFeedService activityFeedService;
  private final PostRepository postRepository;

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
  }

  @KafkaListener(topics = "${kafka.topics.comment-created}", groupId = GROUP_PROFILE_FEED)
  public void consumeCommentCreatedEvent(CommentCreatedEvent event) {
    log.info("Received CommentCreatedEvent: {}", event);
    activityFeedService.createFeedItem(event);
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
