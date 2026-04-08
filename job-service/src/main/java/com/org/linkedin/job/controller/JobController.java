package com.org.linkedin.job.controller;

import com.org.linkedin.dto.ApiResponse;
import com.org.linkedin.dto.job.JobApplicationDTO;
import com.org.linkedin.dto.job.JobDTO;
import com.org.linkedin.job.service.JobService;
import com.org.linkedin.utility.service.AdvanceSearchCriteria;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${apiPrefix}/jobs")
@RequiredArgsConstructor
public class JobController {

  private final JobService jobService;

  @PostMapping
  public ResponseEntity<ApiResponse<JobDTO>> createJob(
      Authentication authentication, @RequestBody JobDTO jobDTO) {
    JobDTO result = jobService.createJob(authentication, jobDTO);
    return ResponseEntity.ok(ApiResponse.success("Job created successfully", result));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<JobDTO>>> getAllJobs(Pageable pageable) {
    Page<JobDTO> serviceRes = jobService.getAllJobs(pageable);
    return ResponseEntity.ok(
        ApiResponse.success(
            "Success",
            serviceRes.getContent(),
            serviceRes.getNumber(),
            serviceRes.getSize(),
            serviceRes.getTotalElements()));
  }

  @GetMapping("/my-postings")
  public ResponseEntity<ApiResponse<List<JobDTO>>> getMyPostings(Authentication authentication) {
    List<JobDTO> result = jobService.getMyPostings(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @GetMapping("/my-applications")
  public ResponseEntity<ApiResponse<List<JobApplicationDTO>>> getMyApplications(
      Authentication authentication) {
    List<JobApplicationDTO> result = jobService.getMyApplications(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @GetMapping("/{jobId}")
  public ResponseEntity<ApiResponse<JobDTO>> getJobById(@PathVariable UUID jobId) {
    JobDTO result = jobService.getJobById(jobId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @PutMapping("/{jobId}")
  public ResponseEntity<ApiResponse<JobDTO>> updateJob(
      Authentication authentication, @PathVariable UUID jobId, @RequestBody JobDTO jobDTO) {
    JobDTO result = jobService.updateJob(authentication, jobId, jobDTO);
    return ResponseEntity.ok(ApiResponse.success("Job updated successfully", result));
  }

  @DeleteMapping("/{jobId}")
  public ResponseEntity<ApiResponse<Void>> deleteJob(Authentication authentication, @PathVariable UUID jobId) {
    jobService.deleteJob(authentication, jobId);
    return ResponseEntity.ok(ApiResponse.success("Job deleted successfully", null));
  }

  @GetMapping("/search")
  public ResponseEntity<ApiResponse<List<JobDTO>>> searchJobs(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String title,
      @RequestParam(required = false) String company,
      @RequestParam(required = false) String location,
      @RequestParam(required = false) String jobType,
      Pageable pageable) {
    Page<JobDTO> serviceRes =
        jobService.searchJobs(query, title, company, location, jobType, pageable);
    return ResponseEntity.ok(
        ApiResponse.success(
            "Success",
            serviceRes.getContent(),
            serviceRes.getNumber(),
            serviceRes.getSize(),
            serviceRes.getTotalElements()));
  }

  @PostMapping("/advanced-search")
  public ResponseEntity<ApiResponse<List<JobDTO>>> advancedSearch(
      @RequestBody AdvanceSearchCriteria criteria) {
    Page<JobDTO> serviceRes = jobService.advancedSearch(criteria);
    return ResponseEntity.ok(
        ApiResponse.success(
            "Success",
            serviceRes.getContent(),
            serviceRes.getNumber(),
            serviceRes.getSize(),
            serviceRes.getTotalElements()));
  }

  @PostMapping("/{jobId}/apply")
  public ResponseEntity<ApiResponse<JobApplicationDTO>> applyToJob(
      Authentication authentication,
      @PathVariable UUID jobId,
      @RequestBody(required = false) Map<String, String> body) {
    String resumeUrl = body != null ? body.get("resumeUrl") : null;
    String coverLetter = body != null ? body.get("coverLetter") : null;
    JobApplicationDTO result = jobService.applyToJob(authentication, jobId, resumeUrl, coverLetter);
    return ResponseEntity.ok(ApiResponse.success("Applied successfully", result));
  }

  @GetMapping("/{jobId}/applicants")
  public ResponseEntity<ApiResponse<List<JobApplicationDTO>>> getJobApplicants(
      @PathVariable UUID jobId, @RequestParam(required = false) String status) {
    List<JobApplicationDTO> result = jobService.getJobApplicants(jobId, status);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  @PatchMapping("/{jobId}/applicants/{applicantId}")
  public ResponseEntity<ApiResponse<JobApplicationDTO>> updateApplicationStatus(
      Authentication authentication,
      @PathVariable UUID jobId,
      @PathVariable UUID applicantId,
      @RequestBody Map<String, String> body) {
    String status = body.get("status");
    JobApplicationDTO result = jobService.updateApplicationStatus(authentication, jobId, applicantId, status);
    return ResponseEntity.ok(ApiResponse.success("Status updated successfully", result));
  }
}
