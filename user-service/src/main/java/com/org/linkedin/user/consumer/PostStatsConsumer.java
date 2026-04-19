package com.org.linkedin.user.consumer;

import com.org.linkedin.dto.event.CommentCreatedEvent;
import com.org.linkedin.dto.event.CommentDeletedEvent;
import com.org.linkedin.dto.event.PostReactedEvent;
import com.org.linkedin.dto.event.PostUnreactedEvent;
import com.org.linkedin.user.repository.PostRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostStatsConsumer {

  private final PostRepository postRepository;

  @KafkaListener(
      topics = "${kafka.topics.post-reacted:post-reacted}",
      groupId = "user-service-stats")
  @Transactional
  public void handlePostReacted(PostReactedEvent event) {
    if (event.isNewReaction()) {
      log.info("Incrementing reaction count for post: {}", event.getPostId());
      postRepository.updateReactionCount(UUID.fromString(event.getPostId()), 1);
    } else {
      log.info("Reaction type changed for post: {}, skipping count increment", event.getPostId());
    }
  }

  @KafkaListener(
      topics = "${kafka.topics.post-unreacted:post-unreacted}",
      groupId = "user-service-stats")
  @Transactional
  public void handlePostUnreacted(PostUnreactedEvent event) {
    log.info("Decrementing reaction count for post: {}", event.getPostId());
    postRepository.updateReactionCount(UUID.fromString(event.getPostId()), -1);
  }

  @KafkaListener(
      topics = "${kafka.topics.comment-created:comment-created}",
      groupId = "user-service-stats")
  @Transactional
  public void handleCommentCreated(CommentCreatedEvent event) {
    log.info("Incrementing comment count for post: {}", event.getPostId());
    postRepository.updateCommentCount(UUID.fromString(event.getPostId()), 1);
  }

  @KafkaListener(
      topics = "${kafka.topics.comment-deleted:comment-deleted}",
      groupId = "user-service-stats")
  @Transactional
  public void handleCommentDeleted(CommentDeletedEvent event) {
    log.info("Decrementing comment count for post: {}", event.getPostId());
    postRepository.updateCommentCount(UUID.fromString(event.getPostId()), -1);
  }
}
