package com.org.linkedin.user.controller;

import com.org.linkedin.domain.enumeration.ReactionType;
import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.PostEnrichmentDTO;
import com.org.linkedin.dto.poll.PollDTO;
import com.org.linkedin.dto.poll.PollOptionDTO;
import com.org.linkedin.user.domain.Post;
import com.org.linkedin.user.dto.CommentDTO;
import com.org.linkedin.user.dto.CommentRequest;
import com.org.linkedin.user.dto.PostDTO;
import com.org.linkedin.user.dto.UpdatePostRequest;
import com.org.linkedin.user.mapper.CommentMapper;
import com.org.linkedin.user.mapper.PostMapper;
import com.org.linkedin.user.service.PostService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for managing user posts, reactions, comments, and polls. All endpoints are
 * prefixed with the service-specific API prefix.
 */
@RestController
@RequestMapping("${apiPrefix}/posts")
public class PostController {

  private final PostService postService;
  private final PostMapper postMapper;
  private final CommentMapper commentMapper;

  public PostController(
      PostService postService, PostMapper postMapper, CommentMapper commentMapper) {
    this.postService = postService;
    this.postMapper = postMapper;
    this.commentMapper = commentMapper;
  }

  /**
   * Creates a new social post with optional image attachments.
   *
   * @param content The text content of the post.
   * @param images Optional list of multipart image files to be attached to the post.
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with the created Post DTO.
   * @throws java.io.IOException if file storage or processing fails.
   */
  @PostMapping
  public ResponseEntity<ApiResponse<PostDTO>> createPost(
      @RequestParam("content") String content,
      @RequestParam(value = "images", required = false) List<MultipartFile> images,
      Authentication authentication)
      throws java.io.IOException {
    Post result = postService.createPost(authentication, content, images);
    return ResponseEntity.ok(
        ApiResponse.success("Post created successfully", postMapper.toDto(result)));
  }

  /**
   * Adds or updates a reaction (Like, Love, etc.) to a specific post.
   *
   * @param postId The unique identifier of the post to react to.
   * @param type The type of reaction to apply (e.g., LIKE, LOVE, etc.).
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @PostMapping("/{postId}/react")
  public ResponseEntity<ApiResponse<Void>> reactToPost(
      @PathVariable UUID postId, @RequestParam ReactionType type, Authentication authentication) {
    postService.reactToPost(authentication, postId, type);
    return ResponseEntity.ok(ApiResponse.success("Reaction added", null));
  }

  /**
   * Removes the current user's reaction from a specific post.
   *
   * @param postId The unique identifier of the post.
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse indicating success.
   */
  @DeleteMapping("/{postId}/react")
  public ResponseEntity<ApiResponse<Void>> unlikePost(
      @PathVariable UUID postId, Authentication authentication) {
    postService.unlikePost(authentication, postId);
    return ResponseEntity.ok(ApiResponse.success("Reaction removed", null));
  }

  /**
   * Retrieves the current user's reaction to a specific post.
   *
   * @param postId The unique identifier of the post.
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with the reaction type or null if no
   *     reaction exists.
   */
  @GetMapping("/{postId}/reaction")
  public ResponseEntity<ApiResponse<ReactionType>> getUserReaction(
      @PathVariable UUID postId, Authentication authentication) {
    ReactionType result = postService.getUserReaction(authentication, postId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Gets the total number of reactions for a specific post.
   *
   * @param postId The unique identifier of the post.
   * @return A ResponseEntity containing an ApiResponse with the total count of reactions.
   */
  @GetMapping("/{postId}/reactions/count")
  public ResponseEntity<ApiResponse<Long>> getReactionCount(@PathVariable UUID postId) {
    long result = postService.getReactionCount(postId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Adds a new comment or a reply to an existing comment on a post.
   *
   * @param postId The unique identifier of the target post.
   * @param parentId Optional ID of the parent comment for threaded replies.
   * @param content The text content of the comment.
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with the created Comment DTO.
   */
  @PostMapping("/{postId}/comments")
  public ResponseEntity<ApiResponse<CommentDTO>> addComment(
      @PathVariable UUID postId,
      @RequestParam(required = false) UUID parentId,
      @RequestBody CommentRequest request,
      Authentication authentication) {
    var result = postService.addComment(authentication, postId, parentId, request.getContent());
    return ResponseEntity.ok(ApiResponse.success("Comment added", commentMapper.toDto(result)));
  }

  /**
   * Deletes a specific comment. Authorization is checked within the service layer.
   *
   * @param commentId The unique identifier of the comment to delete.
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<ApiResponse<Void>> deleteComment(
      @PathVariable UUID commentId, Authentication authentication) {
    postService.deleteComment(authentication, commentId);
    return ResponseEntity.ok(ApiResponse.success("Comment deleted", null));
  }

  /**
   * Retrieves all top-level and nested comments for a specific post.
   *
   * @param postId The unique identifier of the post.
   * @return A ResponseEntity containing an ApiResponse with a list of all associated comment DTOs.
   */
  @GetMapping("/{postId}/comments")
  public ResponseEntity<ApiResponse<List<CommentDTO>>> getComments(@PathVariable UUID postId) {
    var result = postService.getComments(postId);
    return ResponseEntity.ok(ApiResponse.success("Success", commentMapper.toDto(result)));
  }

  /**
   * Retrieves a paginated list of posts created by a specific user.
   *
   * @param userId The unique identifier of the user whose posts are being requested.
   * @param page Zero-based page index.
   * @param size Number of items per page.
   * @return A ResponseEntity containing an ApiResponse with a paginated list of Post DTOs.
   */
  @GetMapping("/user/{userId}")
  public ResponseEntity<ApiResponse<Page<PostDTO>>> getUserPosts(
      @PathVariable UUID userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Authentication authentication) {
    Pageable pageable = PageRequest.of(page, size);
    Page<PostDTO> result = postService.getUserPostsEnriched(authentication, userId, pageable);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Permanently deletes a post and all its associated data (likes, comments, polls).
   *
   * @param postId The unique identifier of the post to delete.
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse indicating success.
   */
  @DeleteMapping("/{postId}")
  public ResponseEntity<ApiResponse<Void>> deletePost(
      @PathVariable UUID postId, Authentication authentication) {
    postService.deletePost(authentication, postId);
    return ResponseEntity.ok(ApiResponse.success("Post deleted", null));
  }

  /**
   * Updates the text content of an existing post.
   *
   * @param postId The unique identifier of the post.
   * @param request Data transfer object containing the new content.
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with the updated Post DTO.
   */
  @PutMapping("/{postId}")
  public ResponseEntity<ApiResponse<PostDTO>> updatePost(
      @PathVariable UUID postId,
      @RequestBody UpdatePostRequest request,
      Authentication authentication) {
    var result = postService.updatePost(authentication, postId, request.getContent());
    return ResponseEntity.ok(ApiResponse.success("Post updated", postMapper.toDto(result)));
  }

  /**
   * Toggles the comment section on or off for a specific post.
   *
   * @param postId The unique identifier of the post.
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with the updated Post DTO reflecting the new
   *     comment state.
   */
  @PostMapping("/{postId}/toggle-comments")
  public ResponseEntity<ApiResponse<PostDTO>> toggleComments(
      @PathVariable UUID postId, Authentication authentication) {
    var result = postService.toggleComments(authentication, postId);
    return ResponseEntity.ok(ApiResponse.success("Comments toggled", postMapper.toDto(result)));
  }

  /**
   * Creates a new interactive poll post.
   *
   * @param pollDTO DTO containing the question, options, and expiry date.
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with the created Post DTO (marked as a
   *     poll).
   */
  @PostMapping("/polls")
  public ResponseEntity<ApiResponse<PostDTO>> createPollPost(
      @RequestBody PollDTO pollDTO, Authentication authentication) {
    var result =
        postService.createPollPost(
            authentication,
            pollDTO.getQuestion(),
            pollDTO.getOptions().stream().map(PollOptionDTO::getText).collect(Collectors.toList()),
            pollDTO.getExpiryDate());
    return ResponseEntity.ok(ApiResponse.success("Poll created", postMapper.toDto(result)));
  }

  /**
   * Records a user's vote for a specific option in a poll.
   *
   * @param postId The ID of the post containing the poll.
   * @param optionId The ID of the selected poll option.
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse indicating success.
   */
  @PostMapping("/{postId}/polls/vote/{optionId}")
  public ResponseEntity<ApiResponse<Void>> voteInPoll(
      @PathVariable UUID postId, @PathVariable UUID optionId, Authentication authentication) {
    postService.voteInPoll(authentication, postId, optionId);
    return ResponseEntity.ok(ApiResponse.success("Vote recorded", null));
  }

  /**
   * Retrieves full details of a poll, including vote counts and the current user's choice.
   *
   * @param postId The unique identifier of the post.
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with a PollDTO containing the latest
   *     results.
   */
  @GetMapping("/{postId}/polls")
  public ResponseEntity<ApiResponse<PollDTO>> getPollDetails(
      @PathVariable UUID postId, Authentication authentication) {
    PollDTO result = postService.getPollDetails(authentication, postId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Industrial-Grade Bulk Enrichment API. Fetches reaction counts, comment counts, and current user
   * reactions for a list of posts in one call. Used to eliminate the N+1 problem in feeds.
   *
   * @param postIds Collection of UUIDs for the posts to enrich.
   * @param authentication The authenticated user security context.
   * @return A ResponseEntity containing an ApiResponse with a PostEnrichmentDTO containing the
   *     batched statistics.
   */
  @PostMapping("/enrichment")
  public ResponseEntity<ApiResponse<PostEnrichmentDTO>> getFeedEnrichment(
      @RequestBody List<UUID> postIds, Authentication authentication) {
    PostEnrichmentDTO enrichment =
        PostEnrichmentDTO.builder()
            .reactionCounts(postService.getReactionCounts(postIds))
            .commentCounts(postService.getCommentCounts(postIds))
            .userReactions(postService.getUserReactions(authentication, postIds))
            .pollOptions(postService.getPollOptions(postIds))
            .hasVoted(postService.getHasVotedMap(authentication, postIds))
            .selectedOptionIds(postService.getSelectedOptionIds(authentication, postIds))
            .build();
    return ResponseEntity.ok(ApiResponse.success("Success", enrichment));
  }
}
