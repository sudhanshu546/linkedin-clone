package com.org.linkedin.search.controller;

import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.search.document.JobDocument;
import com.org.linkedin.search.document.PostDocument;
import com.org.linkedin.search.document.UserDocument;
import com.org.linkedin.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for the Search Engine.
 * Integrates with Elasticsearch to provide high-performance fuzzy search for 
 * professionals and career opportunities.
 */
@RestController
@RequestMapping("${apiPrefix}/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * Performs a fuzzy full-text search for users across names and designations.
     *
     * @param query The search keyword used to find matching users.
     * @param page Zero-based page index for pagination.
     * @param size Size of results per page for pagination.
     * @return A ResponseEntity containing an ApiResponse with a list of matching UserDocuments from Elasticsearch.
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDocument>>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<UserDocument> results = searchService.searchUsers(query, page, size);
        return ResponseEntity.ok(ApiResponse.success("User search results", results));
    }

    /**
     * Performs a fuzzy full-text search for job postings.
     *
     * @param query The search keyword used to find matching job postings.
     * @param page Zero-based page index for pagination.
     * @param size Size of results per page for pagination.
     * @return A ResponseEntity containing an ApiResponse with a list of matching JobDocuments from Elasticsearch.
     */
    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<JobDocument>>> searchJobs(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<JobDocument> results = searchService.searchJobs(query, page, size);
        return ResponseEntity.ok(ApiResponse.success("Job search results", results));
    }

    /**
     * Performs a fuzzy full-text search for posts.
     *
     * @param query The search keyword used to find matching posts.
     * @param page Zero-based page index for pagination.
     * @param size Size of results per page for pagination.
     * @return A ResponseEntity containing an ApiResponse with a list of matching PostDocuments from Elasticsearch.
     */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<PostDocument>>> searchPosts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<PostDocument> results = searchService.searchPosts(query, page, size);
        return ResponseEntity.ok(ApiResponse.success("Post search results", results));
    }

    /**
     * Retrieves the top 10 most popular hashtags across the platform.
     *
     * @return A ResponseEntity containing an ApiResponse with a list of trending hashtag strings.
     */
    @GetMapping("/trending-hashtags")
    public ResponseEntity<ApiResponse<List<String>>> getTrendingHashtags() {
        List<String> results = searchService.getTrendingHashtags();
        return ResponseEntity.ok(ApiResponse.success("Trending hashtags", results));
    }
}
