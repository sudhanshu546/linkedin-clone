package com.org.linkedin.user.service;

import static com.org.linkedin.utility.ProjectConstants.*;

import com.org.linkedin.domain.enumeration.ReactionType;
import com.org.linkedin.dto.event.CommentCreatedEvent;
import com.org.linkedin.dto.event.PostCreatedEvent;
import com.org.linkedin.dto.event.PostDeletedEvent;
import com.org.linkedin.dto.event.PostReactedEvent;
import com.org.linkedin.dto.event.UserMentionedEvent;
import com.org.linkedin.dto.poll.PollDTO;
import com.org.linkedin.dto.poll.PollOptionDTO;
import com.org.linkedin.dto.user.TUserDTO;
import com.org.linkedin.user.domain.*;
import com.org.linkedin.user.mapper.TUserMapper;
import com.org.linkedin.user.repository.*;
import com.org.linkedin.utility.service.KafkaEventPublisher;
import com.org.linkedin.utility.storage.FileStorageService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class PostService {

  private final KafkaEventPublisher kafkaEventPublisher;
  private final UserRepository userRepository;
  private final PostRepository postRepository;
  private final ReactionRepository reactionRepository;
  private final CommentRepository commentRepository;
  private final PollOptionRepository pollOptionRepository;
  private final PollVoteRepository pollVoteRepository;
  private final TUserMapper userMapper;
  private final FileStorageService fileStorageService;

  @Value("${kafka.topics.post-created}")
  private String postCreatedTopic;

  @Value("${kafka.topics.post-reacted:post-reacted}")
  private String postReactedTopic;

  @Value("${kafka.topics.post-unreacted:post-unreacted}")
  private String postUnreactedTopic;

  @Value("${kafka.topics.comment-created}")
  private String commentCreatedTopic;

  @Value("${kafka.topics.user-mentioned:user-mentioned}")
  private String userMentionedTopic;

  @Value("${kafka.topics.post-deleted:post-deleted}")
  private String postDeletedTopic;

  public PostService(
      KafkaEventPublisher kafkaEventPublisher,
      UserRepository userRepository,
      PostRepository postRepository,
      ReactionRepository reactionRepository,
      CommentRepository commentRepository,
      PollOptionRepository pollOptionRepository,
      PollVoteRepository pollVoteRepository,
      TUserMapper userMapper,
      FileStorageService fileStorageService) {
    this.kafkaEventPublisher = kafkaEventPublisher;
    this.userRepository = userRepository;
    this.postRepository = postRepository;
    this.reactionRepository = reactionRepository;
    this.commentRepository = commentRepository;
    this.pollOptionRepository = pollOptionRepository;
    this.pollVoteRepository = pollVoteRepository;
    this.userMapper = userMapper;
    this.fileStorageService = fileStorageService;
  }

  public Post createPollPost(
      Authentication authentication,
      String question,
      List<String> options,
      java.time.LocalDateTime expiryDate) {
    TUserDTO user = getUserDetails(authentication);

    Post post =
        Post.builder()
            .userId(user.getId())
            .isPoll(true)
            .pollQuestion(question)
            .pollExpiryDate(expiryDate)
            .build();

    post = postRepository.save(post);

    for (String optionText : options) {
      PollOption option =
          PollOption.builder()
              .postId(post.getId())
              .text(optionText)
              .voteCount(0L)
              .build();
      pollOptionRepository.save(option);
    }

    String fullName =
        user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");
    String designation = DEFAULT_DESIGNATION;

    // Publish Kafka Event
    try {
      PostCreatedEvent event = new PostCreatedEvent();
      event.setPostId(post.getId().toString());
      event.setUserId(post.getUserId().toString());
      event.setUserName(fullName);
      event.setUserDesignation(designation);
      event.setContent(question);
      event.setPoll(true);
      event.setPollQuestion(question);
      event.setPollOptions(options);
      event.setPollExpiryDate(expiryDate);
      kafkaEventPublisher.publishEvent(postCreatedTopic, event);
    } catch (Exception e) {
      log.error("Failed to publish PostCreatedEvent: {}", e.getMessage());
    }

    return post;
  }

  public void voteInPoll(Authentication authentication, UUID postId, UUID optionId) {
    TUserDTO user = getUserDetails(authentication);

    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));

    if (!post.isPoll()) {
      throw new RuntimeException(ERROR_NOT_A_POLL);
    }

    if (post.getPollExpiryDate() != null
        && post.getPollExpiryDate().isBefore(LocalDateTime.now())) {
      throw new RuntimeException(ERROR_POLL_EXPIRED);
    }

    if (pollVoteRepository.findByPostIdAndUserId(postId, user.getId()).isPresent()) {
      throw new RuntimeException(ERROR_ALREADY_VOTED);
    }

    PollOption option =
        pollOptionRepository
            .findById(optionId)
            .orElseThrow(() -> new RuntimeException(ERROR_OPTION_NOT_FOUND));

    if (!option.getPostId().equals(postId)) {
      throw new RuntimeException(ERROR_OPTION_MISMATCH);
    }

    PollVote vote =
        PollVote.builder()
            .postId(postId)
            .userId(user.getId())
            .optionId(optionId)
            .build();
    pollVoteRepository.save(vote);

    option.setVoteCount(option.getVoteCount() + 1);
    pollOptionRepository.save(option);
  }

  public PollDTO getPollDetails(
      Authentication authentication, UUID postId) {
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));

    if (!post.isPoll()) {
      return null;
    }

    List<PollOption> options =
        pollOptionRepository.findByPostId(postId);
    UUID myVoteOptionId = null;

    if (authentication != null) {
      TUserDTO user = getUserDetails(authentication);
      myVoteOptionId =
          pollVoteRepository
              .findByPostIdAndUserId(postId, user.getId())
              .map(PollVote::getOptionId)
              .orElse(null);
    }

    return PollDTO.builder()
        .id(post.getId())
        .question(post.getPollQuestion())
        .expiryDate(post.getPollExpiryDate())
        .options(
            options.stream()
                .map(
                    o ->
                        PollOptionDTO.builder()
                            .id(o.getId())
                            .text(o.getText())
                            .voteCount(o.getVoteCount())
                            .build())
                .collect(Collectors.toList()))
        .hasVoted(myVoteOptionId != null)
        .selectedOptionId(myVoteOptionId)
        .build();
  }

  public TUserDTO getUserDetails(Authentication authentication) {
    String keycloakId = authentication.getName();
    return userRepository
        .findById(UUID.fromString(keycloakId))
        .map(userMapper::toDto)
        .orElseThrow(() -> new RuntimeException(ERROR_USER_NOT_FOUND));
  }

  public Post createPost(Authentication authentication, String content, List<MultipartFile> images)
      throws IOException {
    TUserDTO user = getUserDetails(authentication);
    List<String> imageUrls = new java.util.ArrayList<>();

    if (images != null) {
      for (MultipartFile image : images) {
        if (!image.isEmpty()) {
          imageUrls.add(fileStorageService.storeFile(image));
        }
      }
    }

    Post post =
        Post.builder()
            .userId(user.getId())
            .content(content)
            .imageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0))
            .imageUrls(imageUrls)
            .build();

    post = postRepository.save(post);

    extractAndPublishHashtags(post.getContent(), post.getId());
    extractAndPublishMentions(post, user);

    String fullName =
        user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");
    String designation = DEFAULT_DESIGNATION;

    // Publish Kafka Event
    try {
      PostCreatedEvent event = new PostCreatedEvent();
      event.setPostId(post.getId().toString());
      event.setUserId(post.getUserId().toString());
      event.setUserName(fullName);
      event.setUserDesignation(designation);
      event.setContent(post.getContent());
      event.setImageUrl(post.getImageUrl());
      event.setImageUrls(post.getImageUrls());
      kafkaEventPublisher.publishEvent(postCreatedTopic, event);
    } catch (Exception e) {
      log.error(
          "Failed to publish PostCreatedEvent for post {}. Error: {}",
          post.getId(),
          e.getMessage());
    }

    return post;
  }

  public void reactToPost(Authentication authentication, UUID postId, ReactionType type) {
    TUserDTO user = getUserDetails(authentication);
    String fullName =
        user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");
    String designation = DEFAULT_DESIGNATION;
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));

    try {
      PostReactedEvent event = new PostReactedEvent();
      event.setPostId(postId.toString());
      event.setPostAuthorId(post.getUserId().toString());
      event.setUserId(user.getId().toString());
      event.setUserName(fullName);
      event.setUserDesignation(designation);
      event.setReactionType(type);
      kafkaEventPublisher.publishEvent(postReactedTopic, event);
    } catch (Exception e) {
      log.error(
          "Failed to publish PostReactedEvent for post {}. Error: {}", postId, e.getMessage());
    }
  }

  private void publishUnreactedEvent(UUID postId, UUID userId) {
    try {
      PostReactedEvent event = new PostReactedEvent();
      event.setPostId(postId.toString());
      event.setUserId(userId.toString());
      kafkaEventPublisher.publishEvent(postUnreactedTopic, event);
    } catch (Exception e) {
      log.error(
          "Failed to publish PostUnreactedEvent for post {}. Error: {}", postId, e.getMessage());
    }
  }

  public long getReactionCount(UUID postId) {
    return reactionRepository.countByPostId(postId);
  }

  public ReactionType getUserReaction(Authentication authentication, UUID postId) {
    if (authentication == null) return null;
    TUserDTO user = getUserDetails(authentication);
    return reactionRepository
        .findByPostIdAndUserId(postId, user.getId())
        .map(Reaction::getType)
        .orElse(null);
  }

  public void unlikePost(Authentication authentication, UUID postId) {
    TUserDTO user = getUserDetails(authentication);
    reactionRepository
        .findByPostIdAndUserId(postId, user.getId())
        .ifPresent(
            reaction -> {
              reactionRepository.delete(reaction);
              publishUnreactedEvent(postId, user.getId());
            });
  }

  public boolean isLikedByUser(Authentication authentication, UUID postId) {
    return getUserReaction(authentication, postId) != null;
  }

  public Comment addComment(
      Authentication authentication, UUID postId, UUID parentId, String content) {
    TUserDTO user = getUserDetails(authentication);
    String fullName =
        user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");
    String designation = DEFAULT_DESIGNATION;

    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));

    if (post.isCommentsDisabled()) {
      throw new RuntimeException("Comments are disabled for this post");
    }

    if (parentId != null) {
      commentRepository
          .findById(parentId)
          .orElseThrow(() -> new RuntimeException(ERROR_PARENT_COMMENT_NOT_FOUND));
    }

    Comment comment =
        Comment.builder()
            .postId(postId)
            .parentId(parentId)
            .userId(user.getId())
            .userName(fullName)
            .userDesignation(designation)
            .content(content)
            .build();
    comment = commentRepository.save(comment);

    try {
      CommentCreatedEvent event = new CommentCreatedEvent();
      event.setCommentId(comment.getId().toString());
      event.setPostId(postId.toString());
      event.setPostAuthorId(post.getUserId().toString());
      event.setUserId(user.getId().toString());
      event.setUserName(fullName);
      event.setUserDesignation(designation);
      event.setContent(content);
      kafkaEventPublisher.publishEvent(commentCreatedTopic, event);
    } catch (Exception e) {
      log.error("Failed to publish CommentCreatedEvent: {}", e.getMessage());
    }

    return comment;
  }

  public List<Comment> getComments(UUID postId) {
    return commentRepository.findByPostIdOrderByCreatedDateDesc(postId);
  }

  public List<Post> getUserPosts(UUID userId) {
    return postRepository.findByUserIdOrderByCreatedDateDesc(userId);
  }

  public org.springframework.data.domain.Page<Post> getUserPostsPaginated(UUID userId, org.springframework.data.domain.Pageable pageable) {
    return postRepository.findByUserIdOrderByCreatedDateDesc(userId, pageable);
  }

  public java.util.Map<UUID, Long> getReactionCounts(java.util.Collection<UUID> postIds) {
    return reactionRepository.findByPostIdIn(postIds).stream()
        .collect(java.util.stream.Collectors.groupingBy(Reaction::getPostId, java.util.stream.Collectors.counting()));
  }

  public java.util.Map<UUID, Long> getCommentCounts(java.util.Collection<UUID> postIds) {
    return commentRepository.findByPostIdIn(postIds).stream()
        .collect(java.util.stream.Collectors.groupingBy(Comment::getPostId, java.util.stream.Collectors.counting()));
  }

  public java.util.Map<UUID, ReactionType> getUserReactions(Authentication authentication, java.util.Collection<UUID> postIds) {
    if (authentication == null) return java.util.Collections.emptyMap();
    TUserDTO user = getUserDetails(authentication);
    return reactionRepository.findByPostIdInAndUserId(postIds, user.getId()).stream()
        .collect(java.util.stream.Collectors.toMap(Reaction::getPostId, Reaction::getType));
  }

  public void deleteComment(Authentication authentication, UUID commentId) {
    TUserDTO user = getUserDetails(authentication);
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new RuntimeException(ERROR_COMMENT_NOT_FOUND));

    Post post = postRepository.findById(comment.getPostId())
        .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));

    // Allow deletion if user is comment author OR post author
    if (!comment.getUserId().equals(user.getId()) && !post.getUserId().equals(user.getId())) {
      throw new RuntimeException(ERROR_UNAUTHORIZED_DELETE_COMMENT);
    }

    commentRepository.delete(comment);
  }

  public Post toggleComments(Authentication authentication, UUID postId) {
    TUserDTO user = getUserDetails(authentication);
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));

    if (!post.getUserId().equals(user.getId())) {
      throw new RuntimeException("Unauthorized to toggle comments");
    }

    post.setCommentsDisabled(!post.isCommentsDisabled());
    return postRepository.save(post);
  }

  public void deletePost(Authentication authentication, UUID postId) {
    TUserDTO user = getUserDetails(authentication);
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));

    if (!post.getUserId().equals(user.getId())) {
      throw new RuntimeException(ERROR_UNAUTHORIZED_DELETE_POST);
    }

    // Delete associated reactions and comments first
    reactionRepository.deleteByPostId(postId);
    commentRepository.deleteByPostId(postId);

    // Cleanup poll data if any
    pollVoteRepository.deleteByPostId(postId);
    pollOptionRepository.deleteByPostId(postId);

    postRepository.delete(post);

    // Notify other services
    try {
      PostDeletedEvent event = new PostDeletedEvent();
      event.setPostId(postId.toString());
      kafkaEventPublisher.publishEvent(postDeletedTopic, event);
    } catch (Exception e) {
      log.error("Failed to publish PostDeletedEvent: {}", e.getMessage());
    }
  }

  public Post updatePost(Authentication authentication, UUID postId, String content) {
    TUserDTO user = getUserDetails(authentication);
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));

    if (!post.getUserId().equals(user.getId())) {
      throw new RuntimeException(ERROR_UNAUTHORIZED_UPDATE_POST);
    }

    post.setContent(content);
    post = postRepository.save(post);
    extractAndPublishHashtags(content, postId);
    return post;
  }

  private void extractAndPublishMentions(Post post, TUserDTO author) {
    String content = post.getContent();
    if (content == null || content.isEmpty()) return;

    // Rich text mentions are in format: <span class="mention-chip" data-user-id="UUID">@Name</span>
    Pattern pattern = Pattern.compile("data-user-id+=\"([^\"]+)\"");
    Matcher matcher = pattern.matcher(content);

    String authorName = author.getFirstName() + " " + author.getLastName();
    String strippedContent = content.replaceAll("<[^>]*>", "");
    String snippet = strippedContent.substring(0, Math.min(strippedContent.length(), 100));

    while (matcher.find()) {
      String mentionedUserId = matcher.group(1);
      try {
        UserMentionedEvent event =
            UserMentionedEvent.builder()
                .postId(post.getId().toString())
                .postAuthorId(author.getId().toString())
                .postAuthorName(authorName)
                .mentionedUserId(mentionedUserId)
                .snippet(snippet)
                .timestamp(System.currentTimeMillis())
                .build();

        kafkaEventPublisher.publishEvent(userMentionedTopic, event);
        log.info("Published mention event for user {} in post {}", mentionedUserId, post.getId());
      } catch (Exception e) {
        log.error("Failed to publish mention event: {}", e.getMessage());
      }
    }
  }

  private void extractAndPublishHashtags(String content, UUID postId) {
    if (content == null || content.isEmpty()) return;
    Pattern pattern = Pattern.compile("#(\\w+)");
    Matcher matcher = pattern.matcher(content);
    List<String> hashtags = new ArrayList<>();
    while (matcher.find()) {
      hashtags.add(matcher.group(1).toLowerCase());
    }
    if (!hashtags.isEmpty()) {
      log.info("Extracted hashtags for post {}: {}", postId, hashtags);
      kafkaEventPublisher.publishEvent(TOPIC_POST_HASHTAGS, hashtags);
    }
  }
}
