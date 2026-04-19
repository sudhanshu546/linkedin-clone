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

/**
 * Controller for managing the Job Portal. Handles job postings, applications, and advanced search
 * functionality.
 */
@RestController
@RequestMapping("${apiPrefix}/jobs")
@RequiredArgsConstructor
public class JobController {

  private final JobService jobService;

  /**
   * Posts a new job opportunity.
   *
   * @param authentication The authenticated recruiter's security context.
   * @param jobDTO DTO containing job title, description, company, and other details.
   * @return A ResponseEntity containing an ApiResponse with the created JobDTO.
   */
  @PostMapping
  public ResponseEntity<ApiResponse<JobDTO>> createJob(
      Authentication authentication, @RequestBody JobDTO jobDTO) {
    JobDTO result = jobService.createJob(authentication, jobDTO);
    return ResponseEntity.ok(ApiResponse.success("Job created successfully", result));
  }

  /**
   * Retrieves all available jobs with pagination.
   *
   * @param pageable Pagination and sorting information.
   * @return A ResponseEntity containing an ApiResponse with a paginated list of JobDTOs.
   */
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

  /**
   * Retrieves all jobs posted by the current authenticated user.
   *
   * @param authentication The authenticated recruiter's security context.
   * @return A ResponseEntity containing an ApiResponse with a list of JobDTOs posted by the user.
   */
  @GetMapping("/my-postings")
  public ResponseEntity<ApiResponse<List<JobDTO>>> getMyPostings(Authentication authentication) {
    List<JobDTO> result = jobService.getMyPostings(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Retrieves all job applications submitted by the current user.
   *
   * @param authentication The authenticated applicant's security context.
   * @return A ResponseEntity containing an ApiResponse with a list of JobApplicationDTOs submitted
   *     by the user.
   */
  @GetMapping("/my-applications")
  public ResponseEntity<ApiResponse<List<JobApplicationDTO>>> getMyApplications(
      Authentication authentication) {
    List<JobApplicationDTO> result = jobService.getMyApplications(authentication);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Retrieves full details of a specific job.
   *
   * @param jobId The unique identifier of the job to retrieve.
   * @return A ResponseEntity containing an ApiResponse with the requested JobDTO.
   */
  @GetMapping("/{jobId}")
  public ResponseEntity<ApiResponse<JobDTO>> getJobById(@PathVariable UUID jobId) {
    JobDTO result = jobService.getJobById(jobId);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Updates an existing job posting.
   *
   * @param authentication The authenticated recruiter's security context.
   * @param jobId The unique identifier of the job to update.
   * @param jobDTO DTO containing the updated job details.
   * @return A ResponseEntity containing an ApiResponse with the updated JobDTO.
   */
  @PutMapping("/{jobId}")
  public ResponseEntity<ApiResponse<JobDTO>> updateJob(
      Authentication authentication, @PathVariable UUID jobId, @RequestBody JobDTO jobDTO) {
    JobDTO result = jobService.updateJob(authentication, jobId, jobDTO);
    return ResponseEntity.ok(ApiResponse.success("Job updated successfully", result));
  }

  /**
   * Deletes a specific job posting.
   *
   * @param authentication The authenticated recruiter's security context.
   * @param jobId The unique identifier of the job to delete.
   * @return A ResponseEntity containing an ApiResponse indicating success with no data.
   */
  @DeleteMapping("/{jobId}")
  public ResponseEntity<ApiResponse<Void>> deleteJob(
      Authentication authentication, @PathVariable UUID jobId) {
    jobService.deleteJob(authentication, jobId);
    return ResponseEntity.ok(ApiResponse.success("Job deleted successfully", null));
  }

  /**
   * Performs a standard search for jobs based on multiple filter criteria.
   *
   * @param query General search keyword.
   * @param title Specific job title to filter by.
   * @param company Company name to filter by.
   * @param location Geographic location to filter by.
   * @param jobType Type of job (e.g., FULL_TIME, PART_TIME) to filter by.
   * @param pageable Pagination and sorting information.
   * @return A ResponseEntity containing an ApiResponse with a paginated list of matching JobDTOs.
   */
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

  /**
   * Performs an advanced search using complex multi-filter criteria.
   *
   * @param criteria The advanced search criteria DTO containing various filters.
   * @return A ResponseEntity containing an ApiResponse with a paginated list of matching JobDTOs.
   */
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

  /**
   * Submits a job application for the current user.
   *
   * @param authentication The authenticated applicant's security context.
   * @param jobId The unique identifier of the job to apply for.
   * @param body A map containing application details such as resumeUrl and coverLetter.
   * @return A ResponseEntity containing an ApiResponse with the created JobApplicationDTO.
   */
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

  /**
   * Retrieves a list of all applicants for a specific job posting.
   *
   * @param jobId The unique identifier of the job posting.
   * @param status Optional status filter to retrieve applicants with a specific application status.
   * @return A ResponseEntity containing an ApiResponse with a list of JobApplicationDTOs for the
   *     job.
   */
  @GetMapping("/{jobId}/applicants")
  public ResponseEntity<ApiResponse<List<JobApplicationDTO>>> getJobApplicants(
      @PathVariable UUID jobId, @RequestParam(required = false) String status) {
    List<JobApplicationDTO> result = jobService.getJobApplicants(jobId, status);
    return ResponseEntity.ok(ApiResponse.success("Success", result));
  }

  /**
   * Updates the status (PENDING, ACCEPTED, REJECTED) of a job application.
   *
   * @param authentication The authenticated recruiter's security context.
   * @param jobId The unique identifier of the job posting.
   * @param applicantId The unique identifier of the applicant/application to update.
   * @param body A map containing the new status value.
   * @return A ResponseEntity containing an ApiResponse with the updated JobApplicationDTO.
   */
  @PatchMapping("/{jobId}/applicants/{applicantId}")
  public ResponseEntity<ApiResponse<JobApplicationDTO>> updateApplicationStatus(
      Authentication authentication,
      @PathVariable UUID jobId,
      @PathVariable UUID applicantId,
      @RequestBody Map<String, String> body) {
    String status = body.get("status");
    String interviewDate = body.get("interviewDate");
    String interviewLink = body.get("interviewLink");
    JobApplicationDTO result =
        jobService.updateApplicationStatus(
            authentication, jobId, applicantId, status, interviewDate, interviewLink);
    return ResponseEntity.ok(ApiResponse.success("Status updated successfully", result));
  }
}
