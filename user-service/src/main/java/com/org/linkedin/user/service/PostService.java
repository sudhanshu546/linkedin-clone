package com.org.linkedin.user.service;

import com.org.linkedin.dto.event.CommentCreatedEvent;
import com.org.linkedin.dto.event.PostCreatedEvent;
import com.org.linkedin.dto.event.PostLikedEvent;
import com.org.linkedin.dto.user.TUserDTO;
import com.org.linkedin.user.domain.Comment;
import com.org.linkedin.user.domain.Like;
import com.org.linkedin.user.domain.Post;
import com.org.linkedin.user.mapper.TUserMapper;
import com.org.linkedin.user.repository.CommentRepository;
import com.org.linkedin.user.repository.LikeRepository;
import com.org.linkedin.user.repository.PostRepository;
import com.org.linkedin.user.repository.UserRepository;
import com.org.linkedin.user.service.storage.FileStorageService;
import com.org.linkedin.utility.service.KafkaEventPublisher;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PostService {

  private final KafkaEventPublisher kafkaEventPublisher;
  private final UserRepository userRepository;
  private final PostRepository postRepository;
  private final LikeRepository likeRepository;
  private final CommentRepository commentRepository;
  private final TUserMapper userMapper;
  private final FileStorageService fileStorageService;

  @Value("${kafka.topics.post-created}")
  private String postCreatedTopic;

  @Value("${kafka.topics.post-liked}")
  private String postLikedTopic;

  @Value("${kafka.topics.post-unliked:post-unliked}")
  private String postUnlikedTopic;

  @Value("${kafka.topics.comment-created}")
  private String commentCreatedTopic;

  public PostService(
      KafkaEventPublisher kafkaEventPublisher,
      UserRepository userRepository,
      PostRepository postRepository,
      LikeRepository likeRepository,
      CommentRepository commentRepository,
      TUserMapper userMapper,
      FileStorageService fileStorageService) {
    this.kafkaEventPublisher = kafkaEventPublisher;
    this.userRepository = userRepository;
    this.postRepository = postRepository;
    this.likeRepository = likeRepository;
    this.commentRepository = commentRepository;
    this.userMapper = userMapper;
    this.fileStorageService = fileStorageService;
  }

  public TUserDTO getUserDetails(Authentication authentication) {
    String keycloakId = authentication.getName();
    return userRepository
        .findById(UUID.fromString(keycloakId))
        .map(userMapper::toDto)
        .orElseThrow(() -> new RuntimeException("User not found"));
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
            .createdAt(LocalDateTime.now())
            .build();

    post = postRepository.save(post);

    String fullName = user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");
    String designation = "LinkedIn Member";

    // Publish Kafka Event
    PostCreatedEvent event = new PostCreatedEvent();
    event.setPostId(post.getId().toString());
    event.setUserId(post.getUserId().toString());
    event.setUserName(fullName);
    event.setUserDesignation(designation);
    event.setContent(post.getContent());
    event.setImageUrl(post.getImageUrl());
    event.setImageUrls(post.getImageUrls());
    kafkaEventPublisher.publishEvent(postCreatedTopic, event);

    return post;
  }

  public void likePost(Authentication authentication, UUID postId) {
    TUserDTO user = getUserDetails(authentication);
    String fullName = user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");
    String designation = "LinkedIn Member";

    if (likeRepository.findByPostIdAndUserId(postId, user.getId()).isPresent()) {
      return;
    }

    Like like = Like.builder().postId(postId).userId(user.getId()).build();
    likeRepository.save(like);

    PostLikedEvent event = new PostLikedEvent();
    event.setPostId(postId.toString());
    event.setUserId(user.getId().toString());
    event.setUserName(fullName);
    event.setUserDesignation(designation);
    kafkaEventPublisher.publishEvent(postLikedTopic, event);
  }

  public long getLikeCount(UUID postId) {
    return likeRepository.countByPostId(postId);
  }

  public Comment addComment(Authentication authentication, UUID postId, String content) {
    TUserDTO user = getUserDetails(authentication);
    String fullName = user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");
    String designation = "LinkedIn Member";

    Comment comment =
        Comment.builder()
            .postId(postId)
            .userId(user.getId())
            .userName(fullName)
            .userDesignation(designation)
            .content(content)
            .createdAt(LocalDateTime.now())
            .build();
    comment = commentRepository.save(comment);

    CommentCreatedEvent event = new CommentCreatedEvent();
    event.setCommentId(comment.getId().toString());
    event.setPostId(postId.toString());
    event.setUserId(user.getId().toString());
    event.setUserName(fullName);
    event.setUserDesignation(designation);
    event.setContent(content);
    kafkaEventPublisher.publishEvent(commentCreatedTopic, event);

    return comment;
  }

  public List<Comment> getComments(UUID postId) {
    return commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
  }

  public void unlikePost(Authentication authentication, UUID postId) {
    TUserDTO user = getUserDetails(authentication);
    likeRepository.findByPostIdAndUserId(postId, user.getId()).ifPresent(likeRepository::delete);

    PostLikedEvent event = new PostLikedEvent();
    event.setPostId(postId.toString());
    event.setUserId(user.getId().toString());
    kafkaEventPublisher.publishEvent(postUnlikedTopic, event);
  }

  public boolean isLikedByUser(Authentication authentication, UUID postId) {
    TUserDTO user = getUserDetails(authentication);
    return likeRepository.findByPostIdAndUserId(postId, user.getId()).isPresent();
  }

  public void deleteComment(Authentication authentication, UUID commentId) {
    TUserDTO user = getUserDetails(authentication);
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new RuntimeException("Comment not found"));
    
    if (!comment.getUserId().equals(user.getId())) {
      throw new RuntimeException("Unauthorized to delete this comment");
    }
    
    commentRepository.delete(comment);
  }
}
