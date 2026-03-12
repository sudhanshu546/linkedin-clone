package com.org.linkedin.job.controller;

import com.org.linkedin.domain.job.Job;
import com.org.linkedin.domain.job.JobApplication;
import com.org.linkedin.job.repository.JobApplicationRepository;
import com.org.linkedin.job.repository.JobRepository;
import com.org.linkedin.utility.client.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${apiPrefix}/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserService userService;

    @PostMapping
    public Job createJob(Authentication authentication, @RequestBody Job job) {
        UUID keycloakId = UUID.fromString(authentication.getName());
        UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();
        job.setPostedBy(internalUserId);
        return jobRepository.save(job);
    }

    @GetMapping
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @GetMapping("/search")
    public List<Job> searchJobs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobType
    ) {
        return jobRepository.searchJobs(title, company, location, jobType);
    }

    @PostMapping("/{jobId}/apply")
    public JobApplication applyToJob(Authentication authentication, @PathVariable UUID jobId) {
        UUID keycloakId = UUID.fromString(authentication.getName());
        UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();
        
        JobApplication application = JobApplication.builder()
                .jobId(jobId)
                .applicantId(internalUserId)
                .status("PENDING")
                .build();
        
        return jobApplicationRepository.save(application);
    }

    @GetMapping("/my-applications")
    public List<JobApplication> getMyApplications(Authentication authentication) {
        UUID keycloakId = UUID.fromString(authentication.getName());
        UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();
        return jobApplicationRepository.findByApplicantIdOrderByAppliedAtDesc(internalUserId);
    }

    @GetMapping("/my-postings")
    public List<Job> getMyPostings(Authentication authentication) {
        UUID keycloakId = UUID.fromString(authentication.getName());
        UUID internalUserId = userService.getUserByKeyCloakId(keycloakId).getBody().getResult().getId();
        return jobRepository.findByPostedByOrderByCreatedAtDesc(internalUserId);
    }

    @GetMapping("/{jobId}/applicants")
    public List<JobApplication> getJobApplicants(@PathVariable UUID jobId) {
        return jobApplicationRepository.findByJobIdOrderByAppliedAtDesc(jobId);
    }

    @PutMapping("/applications/{applicationId}/status")
    public JobApplication updateApplicationStatus(@PathVariable UUID applicationId, @RequestParam String status) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        application.setStatus(status);
        return jobApplicationRepository.save(application);
    }
}
