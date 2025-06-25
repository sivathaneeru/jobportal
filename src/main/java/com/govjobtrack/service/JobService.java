package com.govjobtrack.service;

import com.govjobtrack.payload.request.JobRequest;
import com.govjobtrack.payload.response.JobResponse;
import com.govjobtrack.payload.response.MessageResponse;
import com.govjobtrack.security.services.UserDetailsImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobService {
    JobResponse createJob(JobRequest jobRequest, UserDetailsImpl currentUser);
    JobResponse getJobById(Long jobId);
    Page<JobResponse> getAllJobs(Pageable pageable);
    JobResponse updateJob(Long jobId, JobRequest jobRequest, UserDetailsImpl currentUser);
    MessageResponse deleteJob(Long jobId, UserDetailsImpl currentUser);
}
