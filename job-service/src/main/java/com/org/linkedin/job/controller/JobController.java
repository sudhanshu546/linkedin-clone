package com.org.linkedin.job.controller;

import com.org.linkedin.dto.BasePageResponse;
import com.org.linkedin.dto.BaseResponse;
import com.org.linkedin.dto.job.JobApplicationDTO;
import com.org.linkedin.dto.job.JobDTO;
import com.org.linkedin.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${apiPrefix}/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public BaseResponse<JobDTO> createJob(Authentication authentication, @RequestBody JobDTO jobDTO) {
        JobDTO result = jobService.createJob(authentication, jobDTO);
        return BaseResponse.<JobDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Job created successfully")
                .result(result)
                .build();
    }

    @GetMapping
    public BasePageResponse<List<JobDTO>> getAllJobs(Pageable pageable) {
        return jobService.getAllJobs(pageable);
    }

    @GetMapping("/{jobId}")
    public BaseResponse<JobDTO> getJobById(@PathVariable UUID jobId) {
        JobDTO result = jobService.getJobById(jobId);
        return BaseResponse.<JobDTO>builder()
                .status(HttpStatus.OK.value())
                .result(result)
                .build();
    }

    @PutMapping("/{jobId}")
    public BaseResponse<JobDTO> updateJob(@PathVariable UUID jobId, @RequestBody JobDTO jobDTO) {
        JobDTO result = jobService.updateJob(jobId, jobDTO);
        return BaseResponse.<JobDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Job updated successfully")
                .result(result)
                .build();
    }

    @DeleteMapping("/{jobId}")
    public BaseResponse<Void> deleteJob(@PathVariable UUID jobId) {
        jobService.deleteJob(jobId);
        return BaseResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Job deleted successfully")
                .build();
    }

    @GetMapping("/search")
    public BasePageResponse<List<JobDTO>> searchJobs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobType,
            Pageable pageable
    ) {
        return jobService.searchJobs(query, title, company, location, jobType, pageable);
    }

    @PostMapping("/{jobId}/apply")
    public BaseResponse<JobApplicationDTO> applyToJob(Authentication authentication, @PathVariable UUID jobId) {
        JobApplicationDTO result = jobService.applyToJob(authentication, jobId);
        return BaseResponse.<JobApplicationDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Applied successfully")
                .result(result)
                .build();
    }

    @GetMapping("/my-applications")
    public BaseResponse<List<JobApplicationDTO>> getMyApplications(Authentication authentication) {
        List<JobApplicationDTO> result = jobService.getMyApplications(authentication);
        return BaseResponse.<List<JobApplicationDTO>>builder()
                .status(HttpStatus.OK.value())
                .result(result)
                .build();
    }

    @GetMapping("/my-postings")
    public BaseResponse<List<JobDTO>> getMyPostings(Authentication authentication) {
        List<JobDTO> result = jobService.getMyPostings(authentication);
        return BaseResponse.<List<JobDTO>>builder()
                .status(HttpStatus.OK.value())
                .result(result)
                .build();
    }

    @GetMapping("/{jobId}/applicants")
    public BaseResponse<List<JobApplicationDTO>> getJobApplicants(@PathVariable UUID jobId) {
        List<JobApplicationDTO> result = jobService.getJobApplicants(jobId);
        return BaseResponse.<List<JobApplicationDTO>>builder()
                .status(HttpStatus.OK.value())
                .result(result)
                .build();
    }

    @PatchMapping("/{jobId}/applicants/{applicantId}")
    public BaseResponse<JobApplicationDTO> updateApplicationStatus(
            @PathVariable UUID jobId,
            @PathVariable UUID applicantId,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        JobApplicationDTO result = jobService.updateApplicationStatus(jobId, applicantId, status);
        return BaseResponse.<JobApplicationDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Status updated successfully")
                .result(result)
                .build();
    }
}
