package com.org.linkedin.user.service.impl;

import static com.org.linkedin.utility.ProjectConstants.*;

import com.org.linkedin.domain.enumeration.ReactionType;
import com.org.linkedin.dto.event.*;
import com.org.linkedin.dto.poll.PollDTO;
import com.org.linkedin.dto.poll.PollOptionDTO;
import com.org.linkedin.dto.user.TUserDTO;
import com.org.linkedin.user.domain.*;
import com.org.linkedin.user.dto.PostDTO;
import com.org.linkedin.user.mapper.PostMapper;
import com.org.linkedin.user.mapper.TUserMapper;
import com.org.linkedin.user.repository.*;
import com.org.linkedin.user.service.PostService;
import com.org.linkedin.utility.service.KafkaEventPublisher;
import com.org.linkedin.utility.storage.FileStorageService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

  private final KafkaEventPublisher kafkaEventPublisher;
  private final UserRepository userRepository;
  private final PostRepository postRepository;
  private final ReactionRepository reactionRepository;
  private final CommentRepository commentRepository;
  private final PollOptionRepository pollOptionRepository;
  private final PollVoteRepository pollVoteRepository;
  private final TUserMapper userMapper;
  private final PostMapper postMapper;
  private final FileStorageService fileStorageService;
  private final RedisTemplate<String, Long> redisTemplate;
  private final com.org.linkedin.user.mapper.PollOptionMapper pollOptionMapper;

  private static final String CACHE_REACTION_COUNT = "post:reactions:";
  private static final String CACHE_COMMENT_COUNT = "post:comments:";

  @Value("${kafka.topics.post-created}")
  private String postCreatedTopic;

  @Value("${kafka.topics.post-reacted:post-reacted}")
  private String postReactedTopic;

  @Value("${kafka.topics.post-unreacted:post-unreacted}")
  private String postUnreactedTopic;

  @Value("${kafka.topics.comment-created}")
  private String commentCreatedTopic;

  @Value("${kafka.topics.comment-deleted:comment-deleted}")
  private String commentDeletedTopic;

  @Value("${kafka.topics.user-mentioned:user-mentioned}")
  private String userMentionedTopic;

  @Value("${kafka.topics.post-deleted:post-deleted}")
  private String postDeletedTopic;

  //  public PostServiceImpl(
  //      KafkaEventPublisher kafkaEventPublisher,
  //      UserRepository userRepository,
  //      PostRepository postRepository,
  //      ReactionRepository reactionRepository,
  //      CommentRepository commentRepository,
  //      PollOptionRepository pollOptionRepository,
  //      PollVoteRepository pollVoteRepository,
  //      TUserMapper userMapper,
  //      FileStorageService fileStorageService,
  //      RedisTemplate<String, Long> redisTemplate) {
  //    this.kafkaEventPublisher = kafkaEventPublisher;
  //    this.userRepository = userRepository;
  //    this.postRepository = postRepository;
  //    this.reactionRepository = reactionRepository;
  //    this.commentRepository = commentRepository;
  //    this.pollOptionRepository = pollOptionRepository;
  //    this.pollVoteRepository = pollVoteRepository;
  //    this.userMapper = userMapper;
  //    this.fileStorageService = fileStorageService;
  //    this.redisTemplate = redisTemplate;
  //  }

  // --- Redis Resiliency Helpers ---

  private Long safeGet(String key) {
    try {
      return redisTemplate.opsForValue().get(key);
    } catch (Exception e) {
      log.warn("Redis unavailable for GET: {}", e.getMessage());
      return null;
    }
  }

  private void safeSet(String key, Long value) {
    try {
      redisTemplate.opsForValue().set(key, value, java.time.Duration.ofMinutes(15));
    } catch (Exception e) {
      log.warn("Redis unavailable for SET: {}", e.getMessage());
    }
  }

  private void safeIncrement(String key) {
    try {
      if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
        redisTemplate.opsForValue().increment(key);
      }
    } catch (Exception e) {
      log.warn("Redis unavailable for INCR: {}", e.getMessage());
    }
  }

  private void safeDecrement(String key) {
    try {
      if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
        redisTemplate.opsForValue().decrement(key);
      }
    } catch (Exception e) {
      log.warn("Redis unavailable for DECR: {}", e.getMessage());
    }
  }

  // --- Core Functionality ---

  @Override
  public long getReactionCount(UUID postId) {
    String key = CACHE_REACTION_COUNT + postId.toString();
    Long cached = safeGet(key);
    if (cached != null) return cached;

    return postRepository.findById(postId).map(Post::getReactionCount).orElse(0L);
  }

  @Override
  public Map<UUID, Long> getReactionCounts(Collection<UUID> postIds) {
    if (postIds == null || postIds.isEmpty()) return Collections.emptyMap();

    Map<UUID, String> idToKey =
        postIds.stream()
            .collect(Collectors.toMap(id -> id, id -> CACHE_REACTION_COUNT + id.toString()));
    List<Long> cachedValues = null;
    try {
      cachedValues = redisTemplate.opsForValue().multiGet(idToKey.values());
    } catch (Exception e) {
      log.warn("Redis multiGet failed: {}", e.getMessage());
    }

    Map<UUID, Long> results = new HashMap<>();
    List<UUID> missingIds = new ArrayList<>();

    int i = 0;
    for (UUID id : idToKey.keySet()) {
      Long val = (cachedValues != null && cachedValues.size() > i) ? cachedValues.get(i) : null;
      if (val != null) results.put(id, val);
      else missingIds.add(id);
      i++;
    }

    if (!missingIds.isEmpty()) {
      List<Post> dbPosts = postRepository.findAllById(missingIds);
      for (Post p : dbPosts) {
        results.put(p.getId(), p.getReactionCount());
        safeSet(CACHE_REACTION_COUNT + p.getId().toString(), p.getReactionCount());
      }
    }
    return results;
  }

  @Override
  public Map<UUID, Long> getCommentCounts(Collection<UUID> postIds) {
    if (postIds == null || postIds.isEmpty()) return Collections.emptyMap();
    Map<UUID, String> idToKey =
        postIds.stream()
            .collect(Collectors.toMap(id -> id, id -> CACHE_COMMENT_COUNT + id.toString()));
    List<Long> cachedValues = null;
    try {
      cachedValues = redisTemplate.opsForValue().multiGet(idToKey.values());
    } catch (Exception e) {
      log.warn("Redis multiGet failed: {}", e.getMessage());
    }

    Map<UUID, Long> results = new HashMap<>();
    List<UUID> missingIds = new ArrayList<>();

    int i = 0;
    for (UUID id : idToKey.keySet()) {
      Long val = (cachedValues != null && cachedValues.size() > i) ? cachedValues.get(i) : null;
      if (val != null) results.put(id, val);
      else missingIds.add(id);
      i++;
    }

    if (!missingIds.isEmpty()) {
      List<Post> dbPosts = postRepository.findAllById(missingIds);
      for (Post p : dbPosts) {
        results.put(p.getId(), p.getCommentCount());
        safeSet(CACHE_COMMENT_COUNT + p.getId().toString(), p.getCommentCount());
      }
    }
    return results;
  }

  @Override
  @Transactional
  public void reactToPost(Authentication authentication, UUID postId, ReactionType type) {
    TUserDTO user = getUserDetails(authentication);
    Optional<Reaction> existing = reactionRepository.findByPostIdAndUserId(postId, user.getId());
    boolean isNew = existing.isEmpty();

    if (isNew) {
      reactionRepository.save(
          Reaction.builder()
              .post(postRepository.getReferenceById(postId))
              .userId(user.getId())
              .type(type)
              .build());
      postRepository.updateReactionCount(postId, 1);
    } else {
      Reaction r = existing.get();
      r.setType(type);
      reactionRepository.save(r);
    }

    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));
    try {
      PostReactedEvent event = new PostReactedEvent();
      event.setPostId(postId.toString());
      event.setPostAuthorId(post.getUserId().toString());
      event.setUserId(user.getId().toString());
      event.setUserName(user.getFirstName() + " " + user.getLastName());
      event.setUserDesignation(DEFAULT_DESIGNATION);
      event.setReactionType(type);
      event.setNewReaction(isNew);
      kafkaEventPublisher.publishEvent(postReactedTopic, event);
      if (isNew) safeIncrement(CACHE_REACTION_COUNT + postId.toString());
    } catch (Exception e) {
      log.error("Reaction event failed: {}", e.getMessage());
    }
  }

  @Override
  @Transactional
  public void unlikePost(Authentication authentication, UUID postId) {
    TUserDTO user = getUserDetails(authentication);
    reactionRepository
        .findByPostIdAndUserId(postId, user.getId())
        .ifPresent(
            r -> {
              reactionRepository.delete(r);
              postRepository.updateReactionCount(postId, -1);
              safeDecrement(CACHE_REACTION_COUNT + postId.toString());
              kafkaEventPublisher.publishEvent(
                  postUnreactedTopic,
                  PostUnreactedEvent.builder()
                      .postId(postId.toString())
                      .userId(user.getId().toString())
                      .build());
            });
  }

  @Override
  public ReactionType getUserReaction(Authentication authentication, UUID postId) {
    if (authentication == null) return null;
    TUserDTO user = getUserDetails(authentication);
    return reactionRepository
        .findByPostIdAndUserId(postId, user.getId())
        .map(Reaction::getType)
        .orElse(null);
  }

  @Override
  @Transactional
  public Comment addComment(
      Authentication authentication, UUID postId, UUID parentId, String content) {
    TUserDTO user = getUserDetails(authentication);
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));
    if (post.isCommentsDisabled()) throw new RuntimeException("Comments disabled");

    Comment comment =
        Comment.builder()
            .post(post)
            .parentId(parentId)
            .userId(user.getId())
            .userName(user.getFirstName() + " " + user.getLastName())
            .userDesignation(DEFAULT_DESIGNATION)
            .userProfileImageUrl(user.getProfileImageUrl())
            .content(content)
            .build();

    comment = commentRepository.save(comment);
    postRepository.updateCommentCount(postId, 1);
    safeIncrement(CACHE_COMMENT_COUNT + postId.toString());

    try {
      CommentCreatedEvent event = new CommentCreatedEvent();
      event.setCommentId(comment.getId().toString());
      event.setPostId(postId.toString());
      event.setPostAuthorId(post.getUserId().toString());
      event.setUserId(user.getId().toString());
      event.setUserName(user.getFirstName() + " " + user.getLastName());
      event.setUserDesignation(DEFAULT_DESIGNATION);
      event.setUserProfileImageUrl(user.getProfileImageUrl());
      event.setContent(content);
      kafkaEventPublisher.publishEvent(commentCreatedTopic, event);
    } catch (Exception e) {
      log.error("Comment event failed: {}", e.getMessage());
    }
    return comment;
  }

  @Override
  @Transactional
  public void deleteComment(Authentication authentication, UUID commentId) {
    TUserDTO user = getUserDetails(authentication);
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new RuntimeException(ERROR_COMMENT_NOT_FOUND));

    Post post = comment.getPost();
    if (!comment.getUserId().equals(user.getId())) {
      if (!post.getUserId().equals(user.getId())) {
        throw new RuntimeException(ERROR_UNAUTHORIZED_DELETE_COMMENT);
      }
    }

    UUID postId = post.getId();
    commentRepository.delete(comment);
    postRepository.updateCommentCount(postId, -1);
    safeDecrement(CACHE_COMMENT_COUNT + postId.toString());
    kafkaEventPublisher.publishEvent(
        commentDeletedTopic,
        CommentDeletedEvent.builder()
            .commentId(commentId.toString())
            .postId(postId.toString())
            .build());
  }

  @Override
  public List<Comment> getComments(UUID postId) {
    return commentRepository.findByPostIdOrderByCreatedDateDesc(postId);
  }

  @Override
  @Transactional
  public void deletePost(Authentication authentication, UUID postId) {
    TUserDTO user = getUserDetails(authentication);
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));
    if (!post.getUserId().equals(user.getId()))
      throw new RuntimeException(ERROR_UNAUTHORIZED_DELETE_POST);

    postRepository.delete(post);

    try {
      redisTemplate.delete(CACHE_REACTION_COUNT + postId.toString());
      redisTemplate.delete(CACHE_COMMENT_COUNT + postId.toString());
    } catch (Exception e) {
      log.warn("Redis delete failed: {}", e.getMessage());
    }

    kafkaEventPublisher.publishEvent(
        postDeletedTopic, PostDeletedEvent.builder().postId(postId.toString()).build());
  }

  @Override
  public Post createPost(Authentication authentication, String content, List<MultipartFile> images)
      throws IOException {
    TUserDTO user = getUserDetails(authentication);
    List<String> imageUrls = new ArrayList<>();
    if (images != null)
      for (MultipartFile img : images)
        if (!img.isEmpty()) imageUrls.add(fileStorageService.storeFile(img));

    Post post =
        postRepository.save(
            Post.builder()
                .user(userRepository.getReferenceById(user.getId()))
                .content(content)
                .userProfileImageUrl(user.getProfileImageUrl())
                .imageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0))
                .imageUrls(imageUrls)
                .reactionCount(0L)
                .commentCount(0L)
                .build());

    extractAndPublishHashtags(post.getContent(), post.getId());
    extractAndPublishMentions(post, user);

    try {
      PostCreatedEvent event = new PostCreatedEvent();
      event.setPostId(post.getId().toString());
      event.setUserId(post.getUserId().toString());
      event.setUserName(user.getFirstName() + " " + user.getLastName());
      event.setUserDesignation(DEFAULT_DESIGNATION);
      event.setUserProfileImageUrl(user.getProfileImageUrl());
      event.setContent(post.getContent());
      event.setImageUrl(post.getImageUrl());
      event.setImageUrls(post.getImageUrls());
      kafkaEventPublisher.publishEvent(postCreatedTopic, event);
    } catch (Exception e) {
      log.error("Post creation event failed: {}", e.getMessage());
    }
    return post;
  }

  @Override
  public Post updatePost(Authentication authentication, UUID postId, String content) {
    TUserDTO user = getUserDetails(authentication);
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));
    if (!post.getUserId().equals(user.getId()))
      throw new RuntimeException(ERROR_UNAUTHORIZED_UPDATE_POST);
    post.setContent(content);
    post = postRepository.save(post);
    extractAndPublishHashtags(content, postId);
    return post;
  }

  @Override
  public Post toggleComments(Authentication authentication, UUID postId) {
    TUserDTO user = getUserDetails(authentication);
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));
    if (!post.getUserId().equals(user.getId())) throw new RuntimeException("Unauthorized");
    post.setCommentsDisabled(!post.isCommentsDisabled());
    return postRepository.save(post);
  }

  @Override
  public Page<Post> getUserPostsPaginated(UUID userId, Pageable pageable) {
    return postRepository.findByUserIdOrderByCreatedDateDesc(userId, pageable);
  }

  @Override
  public Page<PostDTO> getUserPostsEnriched(
      Authentication authentication, UUID userId, Pageable pageable) {
    Page<Post> postsPage = postRepository.findByUserIdOrderByCreatedDateDesc(userId, pageable);
    List<PostDTO> enriched = enrichPostDTOs(authentication, postsPage.getContent());
    return new PageImpl<>(enriched, pageable, postsPage.getTotalElements());
  }

  // Helper method to wrap DTO enrichment logic if needed elsewhere
  private List<PostDTO> enrichPostDTOs(Authentication authentication, List<Post> posts) {
    List<UUID> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
    Map<UUID, ReactionType> reactions = getUserReactions(authentication, postIds);
    Map<UUID, List<PollOptionDTO>> pollOpts = getPollOptions(postIds);
    Map<UUID, Boolean> hasVoted = getHasVotedMap(authentication, postIds);
    Map<UUID, UUID> selectedIds = getSelectedOptionIds(authentication, postIds);

    return posts.stream()
        .map(
            p -> {
              PostDTO dto = postMapper.toDto(p);
              if (reactions.containsKey(p.getId())) {
                dto.setLikedByCurrentUser(true);
                dto.setUserReaction(reactions.get(p.getId()).toString());
              }
              dto.setPollOptions(pollOpts.get(p.getId()));
              dto.setHasVoted(Boolean.TRUE.equals(hasVoted.get(p.getId())));
              dto.setSelectedOptionId(selectedIds.get(p.getId()));
              return dto;
            })
        .collect(Collectors.toList());
  }

  @Override
  public List<Post> getUserPosts(UUID userId) {
    return postRepository.findByUserIdOrderByCreatedDateDesc(userId);
  }

  @Override
  public Map<UUID, ReactionType> getUserReactions(
      Authentication authentication, Collection<UUID> postIds) {
    if (authentication == null || postIds == null || postIds.isEmpty())
      return Collections.emptyMap();
    TUserDTO user = getUserDetails(authentication);
    return reactionRepository.findByPostIdInAndUserId(postIds, user.getId()).stream()
        .collect(Collectors.toMap(Reaction::getPostId, Reaction::getType));
  }

  @Override
  @Transactional
  public Post createPollPost(
      Authentication authentication,
      String question,
      List<String> options,
      LocalDateTime expiryDate) {
    TUserDTO user = getUserDetails(authentication);
    Post post =
        postRepository.save(
            Post.builder()
                .user(userRepository.getReferenceById(user.getId()))
                .isPoll(true)
                .pollQuestion(question)
                .pollExpiryDate(expiryDate)
                .userProfileImageUrl(user.getProfileImageUrl())
                .reactionCount(0L)
                .commentCount(0L)
                .build());

    for (String optText : options) {
      pollOptionRepository.save(
          PollOption.builder().postId(post.getId()).post(post).text(optText).voteCount(0L).build());
    }

    try {
      PostCreatedEvent event = new PostCreatedEvent();
      event.setPostId(post.getId().toString());
      event.setUserId(post.getUserId().toString());
      event.setUserName(user.getFirstName() + " " + user.getLastName());
      event.setUserDesignation(DEFAULT_DESIGNATION);
      event.setUserProfileImageUrl(user.getProfileImageUrl());
      event.setContent(question);
      event.setPoll(true);
      event.setPollQuestion(question);
      event.setPollOptions(options);
      event.setPollExpiryDate(expiryDate);
      kafkaEventPublisher.publishEvent(postCreatedTopic, event);
    } catch (Exception e) {
      log.error("Poll event failed: {}", e.getMessage());
    }
    return post;
  }

  @Override
  @Transactional
  public void voteInPoll(Authentication authentication, UUID postId, UUID optionId) {
    TUserDTO user = getUserDetails(authentication);
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));
    if (!post.isPoll()) throw new RuntimeException(ERROR_NOT_A_POLL);
    if (post.getPollExpiryDate() != null && post.getPollExpiryDate().isBefore(LocalDateTime.now()))
      throw new RuntimeException(ERROR_POLL_EXPIRED);

    Optional<PollVote> existingVote =
        pollVoteRepository.findByPostIdAndUserId(postId, user.getId());

    if (existingVote.isPresent()) {
      PollVote vote = existingVote.get();
      if (vote.getOptionId().equals(optionId)) {
        // Toggling off
        pollVoteRepository.delete(vote);
        pollOptionRepository.updateVoteCount(optionId, -1);
      } else {
        // Changing vote
        pollOptionRepository.updateVoteCount(vote.getOptionId(), -1);
        vote.setOptionId(optionId);
        pollVoteRepository.save(vote);
        pollOptionRepository.updateVoteCount(optionId, 1);
      }
    } else {
      // New vote
      pollVoteRepository.save(
          PollVote.builder().postId(postId).userId(user.getId()).optionId(optionId).build());
      pollOptionRepository.updateVoteCount(optionId, 1);
    }
  }

  @Override
  public PollDTO getPollDetails(Authentication authentication, UUID postId) {
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new RuntimeException(ERROR_POST_NOT_FOUND));
    if (!post.isPoll()) return null;

    List<PollOption> options = pollOptionRepository.findByPostId(postId);
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

  @Override
  public TUserDTO getUserDetails(Authentication authentication) {
    String userId = authentication.getName();
    return userRepository
        .findById(UUID.fromString(userId))
        .map(userMapper::toDto)
        .orElseThrow(() -> new RuntimeException(ERROR_USER_NOT_FOUND));
  }

  private void extractAndPublishHashtags(String content, UUID postId) {
    if (content == null || content.isEmpty()) return;
    Pattern pattern = Pattern.compile("#(\\w+)");
    Matcher matcher = pattern.matcher(content);
    List<String> hashtags = new ArrayList<>();
    while (matcher.find()) hashtags.add(matcher.group(1).toLowerCase());
    if (!hashtags.isEmpty()) kafkaEventPublisher.publishEvent(TOPIC_POST_HASHTAGS, hashtags);
  }

  private void extractAndPublishMentions(Post post, TUserDTO author) {
    String content = post.getContent();
    if (content == null || content.isEmpty()) return;

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

  @Override
  public Map<UUID, List<PollOptionDTO>> getPollOptions(Collection<UUID> postIds) {
    if (postIds == null || postIds.isEmpty()) return Collections.emptyMap();
    List<PollOption> options = pollOptionRepository.findByPostIdIn(postIds);
    return options.stream()
        .collect(
            Collectors.groupingBy(
                PollOption::getPostId,
                Collectors.mapping(pollOptionMapper::toDto, Collectors.toList())));
  }

  @Override
  public Map<UUID, Boolean> getHasVotedMap(
      Authentication authentication, Collection<UUID> postIds) {
    if (authentication == null || postIds == null || postIds.isEmpty())
      return Collections.emptyMap();
    TUserDTO user = getUserDetails(authentication);
    List<PollVote> votes = pollVoteRepository.findByPostIdInAndUserId(postIds, user.getId());
    Set<UUID> votedPostIds = votes.stream().map(PollVote::getPostId).collect(Collectors.toSet());

    Map<UUID, Boolean> result = new HashMap<>();
    for (UUID id : postIds) {
      result.put(id, votedPostIds.contains(id));
    }
    return result;
  }

  @Override
  public Map<UUID, UUID> getSelectedOptionIds(
      Authentication authentication, Collection<UUID> postIds) {
    if (authentication == null || postIds == null || postIds.isEmpty())
      return Collections.emptyMap();
    TUserDTO user = getUserDetails(authentication);
    List<PollVote> votes = pollVoteRepository.findByPostIdInAndUserId(postIds, user.getId());
    return votes.stream().collect(Collectors.toMap(PollVote::getPostId, PollVote::getOptionId));
  }
}
