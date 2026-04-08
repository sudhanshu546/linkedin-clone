package com.org.linkedin.user.controller;

import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.poll.PollDTO;
import com.org.linkedin.dto.poll.PollOptionDTO;
import com.org.linkedin.user.domain.Comment;
import com.org.linkedin.user.domain.Post;
import com.org.linkedin.user.service.PostService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${apiPrefix}/posts")
public class PostController {

  private final PostService postService;

  public PostController(PostService postService) {
    this.postService = postService;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<Post>> createPost(
      @RequestParam("content") String content,
      @RequestParam(value = "images", required = false) List<MultipartFile> images,
      Authentication authentication)
      throws java.io.IOException {
    Post result = postService.createPost(authentication, content, images);
    return ResponseEntity.ok(ApiResponse.success("Post created successfully", result));
  }

  @PostMapping("/{postId}/react")
  public ResponseEntity<ApiResponse<Void>> reactToPost(
      @PathVariable UUID postId,
      @RequestParam com.org.linkedin.domain.enumeration.ReactionType type,
      Authentication authentication) {
    postService.reactToPost(authentication, postId, type);
    return ResponseEntity.ok(ApiResponse.success("Reaction added", null));
  }

  @GetMapping("/{postId}/reaction")
  public ResponseEntity<ApiResponse<com.org.linkedin.domain.enumeration.ReactionType>>
      getUserReaction(@PathVariable UUID postId, Authentication authentication) {
    com.org.linkedin.domain.enumeration.ReactionType result =
        postService.getUserReaction(authentication, postId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @GetMapping("/{postId}/reactions/count")
  public ResponseEntity<ApiResponse<Long>> getReactionCount(@PathVariable UUID postId) {
    long result = postService.getReactionCount(postId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @PostMapping("/{postId}/comments")
  public ResponseEntity<ApiResponse<Comment>> addComment(
      @PathVariable UUID postId,
      @RequestParam(required = false) UUID parentId,
      @RequestBody String content,
      Authentication authentication) {
    Comment result = postService.addComment(authentication, postId, parentId, content);
    return ResponseEntity.ok(ApiResponse.success("Comment added", result));
  }

  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<ApiResponse<Void>> deleteComment(
      @PathVariable UUID commentId, Authentication authentication) {
    postService.deleteComment(authentication, commentId);
    return ResponseEntity.ok(ApiResponse.success("Comment deleted", null));
  }

  @GetMapping("/{postId}/comments")
  public ResponseEntity<ApiResponse<List<Comment>>> getComments(@PathVariable UUID postId) {
    List<Comment> result = postService.getComments(postId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @GetMapping("/user/{userId}")
  public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<Post>>> getUserPosts(
      @PathVariable UUID userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
    org.springframework.data.domain.Page<Post> result = postService.getUserPostsPaginated(userId, pageable);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @DeleteMapping("/{postId}")
  public ResponseEntity<ApiResponse<Void>> deletePost(
      @PathVariable UUID postId, Authentication authentication) {
    postService.deletePost(authentication, postId);
    return ResponseEntity.ok(ApiResponse.success("Post deleted", null));
  }

  @PutMapping("/{postId}")
  public ResponseEntity<ApiResponse<Post>> updatePost(
      @PathVariable UUID postId, @RequestBody com.org.linkedin.user.dto.UpdatePostRequest request, Authentication authentication) {
    Post result = postService.updatePost(authentication, postId, request.getContent());
    return ResponseEntity.ok(ApiResponse.success("Post updated", result));
  }

  @PostMapping("/{postId}/toggle-comments")
  public ResponseEntity<ApiResponse<Post>> toggleComments(
      @PathVariable UUID postId, Authentication authentication) {
    Post result = postService.toggleComments(authentication, postId);
    return ResponseEntity.ok(ApiResponse.success("Comments toggled", result));
  }

  @PostMapping("/polls")
  public ResponseEntity<ApiResponse<Post>> createPollPost(
          @RequestBody PollDTO pollDTO, Authentication authentication) {
    Post result =
        postService.createPollPost(
            authentication,
            pollDTO.getQuestion(),
            pollDTO.getOptions().stream()
                .map(PollOptionDTO::getText)
                .collect(Collectors.toList()),
            pollDTO.getExpiryDate());
    return ResponseEntity.ok(ApiResponse.success("Poll created", result));
  }

  @PostMapping("/{postId}/polls/vote/{optionId}")
  public ResponseEntity<ApiResponse<Void>> voteInPoll(
      @PathVariable UUID postId, @PathVariable UUID optionId, Authentication authentication) {
    postService.voteInPoll(authentication, postId, optionId);
    return ResponseEntity.ok(ApiResponse.success("Vote recorded", null));
  }

  @GetMapping("/{postId}/polls")
  public ResponseEntity<ApiResponse<com.org.linkedin.dto.poll.PollDTO>> getPollDetails(
      @PathVariable UUID postId, Authentication authentication) {
    com.org.linkedin.dto.poll.PollDTO result = postService.getPollDetails(authentication, postId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @PostMapping("/enrichment")
  public ResponseEntity<ApiResponse<com.org.linkedin.dto.PostEnrichmentDTO>> getFeedEnrichment(
      @RequestBody List<UUID> postIds, Authentication authentication) {
    com.org.linkedin.dto.PostEnrichmentDTO enrichment =
        com.org.linkedin.dto.PostEnrichmentDTO.builder()
            .reactionCounts(postService.getReactionCounts(postIds))
            .commentCounts(postService.getCommentCounts(postIds))
            .userReactions(postService.getUserReactions(authentication, postIds))
            .build();
    return ResponseEntity.ok(ApiResponse.success("Success", enrichment));
  }
}
