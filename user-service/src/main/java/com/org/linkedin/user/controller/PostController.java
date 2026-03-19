package com.org.linkedin.user.controller;

import com.org.linkedin.user.domain.Comment;
import com.org.linkedin.user.domain.Post;
import com.org.linkedin.user.service.PostService;
import java.util.List;
import java.util.UUID;
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
  public Post createPost(
      @RequestParam("content") String content,
      @RequestParam(value = "images", required = false) List<MultipartFile> images,
      Authentication authentication)
      throws java.io.IOException {
    return postService.createPost(authentication, content, images);
  }

  @PostMapping("/{postId}/like")
  public void likePost(@PathVariable UUID postId, Authentication authentication) {
    postService.likePost(authentication, postId);
  }

  @DeleteMapping("/{postId}/unlike")
  public void unlikePost(@PathVariable UUID postId, Authentication authentication) {
    postService.unlikePost(authentication, postId);
  }

  @GetMapping("/{postId}/is-liked")
  public boolean isLiked(@PathVariable UUID postId, Authentication authentication) {
    return postService.isLikedByUser(authentication, postId);
  }

  @GetMapping("/{postId}/likes/count")
  public long getLikeCount(@PathVariable UUID postId) {
    return postService.getLikeCount(postId);
  }

  @PostMapping("/{postId}/comments")
  public Comment addComment(
      @PathVariable UUID postId, @RequestBody String content, Authentication authentication) {
    return postService.addComment(authentication, postId, content);
  }

  @DeleteMapping("/comments/{commentId}")
  public void deleteComment(@PathVariable UUID commentId, Authentication authentication) {
    postService.deleteComment(authentication, commentId);
  }

  @GetMapping("/{postId}/comments")
  public List<Comment> getComments(@PathVariable UUID postId) {
    return postService.getComments(postId);
  }

  @DeleteMapping("/{postId}")
  public void deletePost(@PathVariable UUID postId, Authentication authentication) {
    postService.deletePost(authentication, postId);
  }

  @PutMapping("/{postId}")
  public Post updatePost(
      @PathVariable UUID postId, @RequestBody String content, Authentication authentication) {
    return postService.updatePost(authentication, postId, content);
  }
}
