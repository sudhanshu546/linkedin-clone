package com.org.linkedin.search.controller;

import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.search.document.JobDocument;
import com.org.linkedin.search.document.UserDocument;
import com.org.linkedin.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${apiPrefix}/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDocument>>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<UserDocument> results = searchService.searchUsers(query, page, size);
        return ResponseEntity.ok(ApiResponse.success("User search results", results));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<JobDocument>>> searchJobs(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<JobDocument> results = searchService.searchJobs(query, page, size);
        return ResponseEntity.ok(ApiResponse.success("Job search results", results));
    }

    @GetMapping("/trending-hashtags")
    public ResponseEntity<ApiResponse<List<String>>> getTrendingHashtags() {
        List<String> results = searchService.getTrendingHashtags();
        return ResponseEntity.ok(ApiResponse.success("Trending hashtags", results));
    }
}
