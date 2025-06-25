package com.govjobtrack.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.govjobtrack.model.Job;
import com.govjobtrack.model.Role;
import com.govjobtrack.model.RoleEntity;
import com.govjobtrack.model.User;
import com.govjobtrack.payload.request.JobRequest;
import com.govjobtrack.payload.response.JobResponse;
import com.govjobtrack.repository.JobRepository;
import com.govjobtrack.repository.RoleRepository;
import com.govjobtrack.repository.UserRepository;
import com.govjobtrack.security.jwt.JwtUtils; // To generate tokens for tests
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page; // For deserializing Page
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Rollback transactions after each test
public class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // Spring Boot auto-configures JavaTimeModule

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils; // To generate tokens

    private String adminToken;
    private String userToken;
    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule()); // Ensure Java 8 date/time is handled

        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        RoleEntity adminRole = roleRepository.save(new RoleEntity(Role.ROLE_ADMIN));
        RoleEntity userRole = roleRepository.save(new RoleEntity(Role.ROLE_USER));

        adminUser = new User("Admin", "Test", "admin@test.com", passwordEncoder.encode("password"));
        adminUser.setRoles(Collections.singleton(adminRole));
        userRepository.save(adminUser);
        adminToken = "Bearer " + jwtUtils.generateTokenFromEmail(adminUser.getEmail());

        regularUser = new User("User", "Test", "user@test.com", passwordEncoder.encode("password"));
        regularUser.setRoles(Collections.singleton(userRole));
        userRepository.save(regularUser);
        userToken = "Bearer " + jwtUtils.generateTokenFromEmail(regularUser.getEmail());
    }

    @Test
    void createJob_asAdmin_success() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setTitle("Software Engineer");
        jobRequest.setDepartment("Tech");
        jobRequest.setDescription("Develop awesome stuff");
        jobRequest.setQualification("B.S. CS");
        jobRequest.setLastDateToApply(LocalDate.now().plusMonths(1));

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Software Engineer"))
                .andExpect(jsonPath("$.department").value("Tech"))
                .andExpect(jsonPath("$.createdByUsername").value(adminUser.getEmail()));

        assertThat(jobRepository.count()).isEqualTo(1);
    }

    @Test
    void createJob_asUser_forbidden() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setTitle("Denied Job");
        jobRequest.setDepartment("HR");
        jobRequest.setDescription("This won't work");
        jobRequest.setQualification("N/A");
        jobRequest.setLastDateToApply(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", userToken) // Regular user token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobRequest)))
                .andExpect(status().isForbidden()); // Expect 403 Forbidden
    }

    @Test
    void createJob_withoutToken_unauthorized() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setTitle("No Auth Job");
        // ... set other fields
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobRequest)))
                .andExpect(status().isUnauthorized()); // Expect 401 Unauthorized
    }


    @Test
    void getJobById_publicAccess_success() throws Exception {
        Job job = new Job(null, "Public Job", "Public Dept", "Desc", "Qual", null, LocalDate.now().plusDays(5), null, adminUser);
        Job savedJob = jobRepository.save(job);

        mockMvc.perform(get("/api/jobs/" + savedJob.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedJob.getId()))
                .andExpect(jsonPath("$.title").value("Public Job"));
    }

    @Test
    void getJobById_notFound() throws Exception {
        mockMvc.perform(get("/api/jobs/9999") // Non-existent ID
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job not found with id : '9999'"));
    }

    @Test
    void getAllJobs_publicAccess_successWithPagination() throws Exception {
        jobRepository.save(new Job(null, "Job 1", "Dept A", "D1", "Q1", null, LocalDate.now().plusDays(1), null, adminUser));
        jobRepository.save(new Job(null, "Job 2", "Dept B", "D2", "Q2", null, LocalDate.now().plusDays(2), null, adminUser));

        MvcResult result = mockMvc.perform(get("/api/jobs?page=0&size=1&sort=title,asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Job 1")) // Assuming Job 1 sorts before Job 2 by title
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(1))
                .andReturn();

        // For full Page deserialization (more complex, requires custom handling or specific PageJsonSerializer)
        // String content = result.getResponse().getContentAsString();
        // Page<JobResponse> pageResponse = objectMapper.readValue(content, new TypeReference<RestResponsePage<JobResponse>>() {});
        // assertThat(pageResponse.getContent()).hasSize(1);
    }

    @Test
    void updateJob_asAdmin_success() throws Exception {
        Job job = new Job(null, "Old Title", "Old Dept", "Old Desc", "Old Qual", null, LocalDate.now().plusDays(3), null, adminUser);
        Job savedJob = jobRepository.save(job);

        JobRequest updateRequest = new JobRequest();
        updateRequest.setTitle("New Title");
        updateRequest.setDepartment("New Dept");
        updateRequest.setDescription("New Desc");
        updateRequest.setQualification("New Qual");
        updateRequest.setLastDateToApply(LocalDate.now().plusMonths(2));


        mockMvc.perform(put("/api/jobs/" + savedJob.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.department").value("New Dept"));

        Job updatedJob = jobRepository.findById(savedJob.getId()).get();
        assertThat(updatedJob.getTitle()).isEqualTo("New Title");
    }

    @Test
    void updateJob_asAdmin_jobNotFound() throws Exception {
        JobRequest updateRequest = new JobRequest();
        updateRequest.setTitle("Non Existent Update");
        // ... set other fields

        mockMvc.perform(put("/api/jobs/8888") // Non-existent ID
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }


    @Test
    void deleteJob_asAdmin_success() throws Exception {
        Job job = new Job(null, "To Delete", "Temp Dept", "Desc", "Qual", null, LocalDate.now().plusDays(1), null, adminUser);
        Job savedJob = jobRepository.save(job);

        assertThat(jobRepository.count()).isEqualTo(1);

        mockMvc.perform(delete("/api/jobs/" + savedJob.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Job deleted successfully!"));

        assertThat(jobRepository.count()).isEqualTo(0);
    }

    @Test
    void deleteJob_asUser_forbidden() throws Exception {
        Job job = new Job(null, "Protected Delete", "Dept", "Desc", "Qual", null, LocalDate.now().plusDays(1), null, adminUser);
        Job savedJob = jobRepository.save(job);

        mockMvc.perform(delete("/api/jobs/" + savedJob.getId())
                        .header("Authorization", userToken)) // User token
                .andExpect(status().isForbidden());

        assertThat(jobRepository.existsById(savedJob.getId())).isTrue(); // Job should still exist
    }
}

// Helper class for deserializing Page<T> if needed, Spring Boot provides one with Hateoas usually.
// For simple cases, checking path expressions is enough.
// If using Jackson to deserialize Page, you need a concrete class like this:
/*
class RestResponsePage<T> extends org.springframework.data.domain.PageImpl<T> {
    public RestResponsePage(java.util.List<T> content, org.springframework.data.domain.Pageable pageable, long total) {
        super(content, pageable, total);
    }
    public RestResponsePage(java.util.List<T> content) {
        super(content);
    }
    public RestResponsePage() {
        super(new java.util.ArrayList<T>());
    }
}
*/
