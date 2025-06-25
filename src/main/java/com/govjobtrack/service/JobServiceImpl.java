package com.govjobtrack.service;

import com.govjobtrack.exception.ResourceNotFoundException;
import com.govjobtrack.model.Job;
import com.govjobtrack.model.User;
import com.govjobtrack.payload.request.JobRequest;
import com.govjobtrack.payload.response.JobResponse;
import com.govjobtrack.payload.response.MessageResponse;
import com.govjobtrack.repository.JobRepository;
import com.govjobtrack.repository.UserRepository;
import com.govjobtrack.security.services.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class JobServiceImpl implements JobService {

    private static final Logger logger = LoggerFactory.getLogger(JobServiceImpl.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository; // To fetch the User entity for 'createdBy'

    // --- Mapper method (can be moved to a dedicated mapper class later) ---
    private JobResponse mapJobToJobResponse(Job job) {
        if (job == null) {
            return null;
        }
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getDepartment(),
                job.getDescription(),
                job.getQualification(),
                job.getApplicationLink(),
                job.getLastDateToApply(),
                job.getPostedDate(),
                job.getCreatedBy() != null ? job.getCreatedBy().getEmail() : "N/A", // Username (email)
                job.getCreatedBy() != null ? job.getCreatedBy().getId() : null // User ID
        );
    }


    @Override
    @Transactional
    public JobResponse createJob(JobRequest jobRequest, UserDetailsImpl currentUserDetails) {
        User currentUser = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserDetails.getId()));

        Job job = new Job();
        job.setTitle(jobRequest.getTitle());
        job.setDepartment(jobRequest.getDepartment());
        job.setDescription(jobRequest.getDescription());
        job.setQualification(jobRequest.getQualification());
        job.setApplicationLink(jobRequest.getApplicationLink());
        job.setLastDateToApply(jobRequest.getLastDateToApply());
        job.setCreatedBy(currentUser);
        // postedDate is set by @CreatedDate

        Job savedJob = jobRepository.save(job);
        logger.info("Job created with ID: {} by User ID: {}", savedJob.getId(), currentUser.getId());
        return mapJobToJobResponse(savedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));
        return mapJobToJobResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> getAllJobs(Pageable pageable) {
        Page<Job> jobsPage = jobRepository.findAll(pageable);
        return jobsPage.map(this::mapJobToJobResponse);
    }

    @Override
    @Transactional
    public JobResponse updateJob(Long jobId, JobRequest jobRequest, UserDetailsImpl currentUserDetails) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        User currentUser = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserDetails.getId()));

        // Authorization check: Only the user who created the job or any admin can update.
        // For this app, any admin can edit any job. If only creator, check:
        // if (!job.getCreatedBy().getId().equals(currentUser.getId())) {
        //     logger.warn("User ID {} attempted to update job ID {} not owned by them.", currentUser.getId(), jobId);
        //     throw new AccessDeniedException("You are not authorized to update this job.");
        // }
        // Since @PreAuthorize on controller handles role check, no need for explicit role check here if any admin can edit.

        job.setTitle(jobRequest.getTitle());
        job.setDepartment(jobRequest.getDepartment());
        job.setDescription(jobRequest.getDescription());
        job.setQualification(jobRequest.getQualification());
        job.setApplicationLink(jobRequest.getApplicationLink());
        job.setLastDateToApply(jobRequest.getLastDateToApply());
        // createdBy and postedDate should not change on update

        Job updatedJob = jobRepository.save(job);
        logger.info("Job ID: {} updated by User ID: {}", updatedJob.getId(), currentUser.getId());
        return mapJobToJobResponse(updatedJob);
    }

    @Override
    @Transactional
    public MessageResponse deleteJob(Long jobId, UserDetailsImpl currentUserDetails) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        User currentUser = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserDetails.getId()));

        // Authorization check (similar to update)
        // if (!job.getCreatedBy().getId().equals(currentUser.getId())) {
        //     logger.warn("User ID {} attempted to delete job ID {} not owned by them.", currentUser.getId(), jobId);
        //     throw new AccessDeniedException("You are not authorized to delete this job.");
        // }
        // Again, @PreAuthorize on controller handles role.

        jobRepository.delete(job);
        logger.info("Job ID: {} deleted by User ID: {}", jobId, currentUser.getId());
        return new MessageResponse("Job deleted successfully!");
    }
}
