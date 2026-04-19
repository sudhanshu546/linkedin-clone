package com.org.linkedin.user.service;

import com.org.linkedin.domain.enumeration.ReactionType;
import com.org.linkedin.dto.poll.PollDTO;
import com.org.linkedin.dto.user.TUserDTO;
import com.org.linkedin.user.domain.Comment;
import com.org.linkedin.user.domain.Post;
import com.org.linkedin.user.dto.PostDTO;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for all Post-related business logic. Handles social interactions including
 * posts, reactions, comments, and polls.
 */
public interface PostService {

  /**
   * Creates a standard social post.
   *
   * @param authentication User security context.
   * @param content Post text.
   * @param images List of image files to upload.
   * @return The persisted post.
   * @throws IOException if file storage fails.
   */
  Post createPost(Authentication authentication, String content, List<MultipartFile> images)
      throws IOException;

  /** Updates an existing post's text. */
  Post updatePost(Authentication authentication, UUID postId, String content);

  /** Permanently deletes a post and cleans up all related data. */
  void deletePost(Authentication authentication, UUID postId);

  /** Adds or updates a user reaction. */
  void reactToPost(Authentication authentication, UUID postId, ReactionType type);

  /** Removes a user's reaction from a post. */
  void unlikePost(Authentication authentication, UUID postId);

  /** Returns the reaction type for a specific user and post. */
  ReactionType getUserReaction(Authentication authentication, UUID postId);

  /** Returns total reactions for a post (pre-aggregated/cached). */
  long getReactionCount(UUID postId);

  /** Bulk fetch reaction counts for a list of posts (Redis Multi-Get optimized). */
  Map<UUID, Long> getReactionCounts(Collection<UUID> postIds);

  /** Bulk fetch user reactions for a list of posts. */
  Map<UUID, ReactionType> getUserReactions(Authentication authentication, Collection<UUID> postIds);

  /** Adds a comment or reply to a post. */
  Comment addComment(Authentication authentication, UUID postId, UUID parentId, String content);

  /** Deletes a comment. Authorized for comment author or post owner. */
  void deleteComment(Authentication authentication, UUID commentId);

  /** Retrieves all comments for a post. */
  List<Comment> getComments(UUID postId);

  /** Bulk fetch comment counts for a list of posts. */
  Map<UUID, Long> getCommentCounts(Collection<UUID> postIds);

  /** Enables or disables commenting on a post. */
  Post toggleComments(Authentication authentication, UUID postId);

  /** Paginated retrieval of posts by a specific user. */
  Page<Post> getUserPostsPaginated(UUID userId, Pageable pageable);

  /** Paginated retrieval of posts by a specific user with enrichment for the viewer. */
  Page<PostDTO> getUserPostsEnriched(Authentication authentication, UUID userId, Pageable pageable);

  /** List retrieval of posts by a specific user. */
  List<Post> getUserPosts(UUID userId);

  /** Creates a poll post. */
  Post createPollPost(
      Authentication authentication,
      String question,
      List<String> options,
      LocalDateTime expiryDate);

  /** Submits a vote for a poll option. */
  void voteInPoll(Authentication authentication, UUID postId, UUID optionId);

  /** Retrieves poll data with vote percentages and user state. */
  PollDTO getPollDetails(Authentication authentication, UUID postId);

  /** Bulk fetch poll options for a list of posts. */
  Map<UUID, List<com.org.linkedin.dto.poll.PollOptionDTO>> getPollOptions(Collection<UUID> postIds);

  /** Bulk fetch whether the user has voted in a list of polls. */
  Map<UUID, Boolean> getHasVotedMap(Authentication authentication, Collection<UUID> postIds);

  /** Bulk fetch which option the user selected in a list of polls. */
  Map<UUID, UUID> getSelectedOptionIds(Authentication authentication, Collection<UUID> postIds);

  /** Maps authentication context to a User DTO. */
  TUserDTO getUserDetails(Authentication authentication);
}
