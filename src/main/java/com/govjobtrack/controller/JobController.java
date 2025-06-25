package com.govjobtrack.controller;

import com.govjobtrack.payload.request.JobRequest;
import com.govjobtrack.payload.response.JobResponse;
import com.govjobtrack.payload.response.MessageResponse;
import com.govjobtrack.security.services.UserDetailsImpl;
import com.govjobtrack.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest jobRequest,
                                                 @AuthenticationPrincipal UserDetailsImpl currentUser) {
        JobResponse jobResponse = jobService.createJob(jobRequest, currentUser);
        return new ResponseEntity<>(jobResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable Long jobId) {
        JobResponse jobResponse = jobService.getJobById(jobId);
        return ResponseEntity.ok(jobResponse);
    }

    @GetMapping
    public ResponseEntity<Page<JobResponse>> getAllJobs(@PageableDefault(size = 10, sort = "postedDate") Pageable pageable) {
        // Example of @PageableDefault to set default size and sort.
        // Client can override by passing ?page=0&size=5&sort=title,asc
        Page<JobResponse> jobsPage = jobService.getAllJobs(pageable);
        return ResponseEntity.ok(jobsPage);
    }

    @PutMapping("/{jobId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobResponse> updateJob(@PathVariable Long jobId,
                                                 @Valid @RequestBody JobRequest jobRequest,
                                                 @AuthenticationPrincipal UserDetailsImpl currentUser) {
        JobResponse jobResponse = jobService.updateJob(jobId, jobRequest, currentUser);
        return ResponseEntity.ok(jobResponse);
    }

    @DeleteMapping("/{jobId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteJob(@PathVariable Long jobId,
                                                     @AuthenticationPrincipal UserDetailsImpl currentUser) {
        MessageResponse messageResponse = jobService.deleteJob(jobId, currentUser);
        return ResponseEntity.ok(messageResponse);
    }
}
