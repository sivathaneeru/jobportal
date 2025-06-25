package com.govjobtrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.govjobtrack.model.*; // Job, User, RoleEntity, Bookmark
import com.govjobtrack.repository.BookmarkRepository;
import com.govjobtrack.repository.JobRepository;
import com.govjobtrack.repository.RoleRepository;
import com.govjobtrack.repository.UserRepository;
import com.govjobtrack.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Rollback transactions after each test
public class BookmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private String adminToken;
    private String userToken;
    private User adminUser;
    private User regularUser;
    private Job testJob1;
    private Job testJob2;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());

        bookmarkRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        RoleEntity adminRole = roleRepository.save(new RoleEntity(Role.ROLE_ADMIN));
        RoleEntity userRole = roleRepository.save(new RoleEntity(Role.ROLE_USER));

        adminUser = new User("AdminBM", "TestBM", "adminbm@test.com", passwordEncoder.encode("password"));
        adminUser.setRoles(Collections.singleton(adminRole));
        userRepository.save(adminUser);
        adminToken = "Bearer " + jwtUtils.generateTokenFromEmail(adminUser.getEmail());

        regularUser = new User("UserBM", "TestBM", "userbm@test.com", passwordEncoder.encode("password"));
        regularUser.setRoles(Collections.singleton(userRole));
        userRepository.save(regularUser);
        userToken = "Bearer " + jwtUtils.generateTokenFromEmail(regularUser.getEmail());

        testJob1 = jobRepository.save(new Job(null, "Job Alpha", "Dept Alpha", "Desc A", "Qual A", null, LocalDate.now().plusDays(10), null, adminUser));
        testJob2 = jobRepository.save(new Job(null, "Job Beta", "Dept Beta", "Desc B", "Qual B", null, LocalDate.now().plusDays(20), null, adminUser));
    }

    @Test
    void addBookmark_asUser_success() throws Exception {
        mockMvc.perform(post("/api/bookmarks/job/" + testJob1.getId())
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId").value(testJob1.getId()))
                .andExpect(jsonPath("$.userId").value(regularUser.getId()))
                .andExpect(jsonPath("$.jobTitle").value("Job Alpha"));

        assertThat(bookmarkRepository.findByUserAndJob(regularUser, testJob1)).isPresent();
    }

    @Test
    void addBookmark_asUser_jobNotFound() throws Exception {
        mockMvc.perform(post("/api/bookmarks/job/99999") // Non-existent job ID
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job not found with id : '99999'"));
    }

    @Test
    void addBookmark_asUser_alreadyBookmarked() throws Exception {
        // First, bookmark the job
        bookmarkRepository.save(new Bookmark(regularUser, testJob1));

        // Attempt to bookmark again
        mockMvc.perform(post("/api/bookmarks/job/" + testJob1.getId())
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict()) // 409 Conflict for BookmarkAlreadyExistsException
                .andExpect(jsonPath("$.message").value(String.format("Bookmark already exists for user ID '%d' and job ID '%d'", regularUser.getId(), testJob1.getId())));
    }

    @Test
    void addBookmark_asAdmin_forbidden() throws Exception {
        // Admins are not supposed to bookmark jobs as per current role setup for this endpoint
        mockMvc.perform(post("/api/bookmarks/job/" + testJob1.getId())
                        .header("Authorization", adminToken) // Admin token
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void addBookmark_withoutToken_unauthorized() throws Exception {
        mockMvc.perform(post("/api/bookmarks/job/" + testJob1.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void removeBookmark_asUser_success() throws Exception {
        Bookmark bookmark = bookmarkRepository.save(new Bookmark(regularUser, testJob1));
        assertThat(bookmarkRepository.findById(bookmark.getId())).isPresent();

        mockMvc.perform(delete("/api/bookmarks/job/" + testJob1.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bookmark removed successfully!"));

        assertThat(bookmarkRepository.findById(bookmark.getId())).isNotPresent();
    }

    @Test
    void removeBookmark_asUser_bookmarkNotFound() throws Exception {
        // Attempt to remove a bookmark that doesn't exist for this user/job
        mockMvc.perform(delete("/api/bookmarks/job/" + testJob2.getId()) // testJob2 is not bookmarked by regularUser
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format("Bookmark not found for user ID '%d' and job ID '%d'", regularUser.getId(), testJob2.getId())));
    }

    @Test
    void getUserBookmarks_asUser_success() throws Exception {
        bookmarkRepository.save(new Bookmark(regularUser, testJob1));
        bookmarkRepository.save(new Bookmark(regularUser, testJob2));

        // Another user's bookmark, should not appear
        User otherUser = userRepository.save(new User("Other", "User", "other@test.com", passwordEncoder.encode("password"), Collections.singleton(roleRepository.findByName(Role.ROLE_USER).get())));
        Job otherJob = jobRepository.save(new Job(null, "Job Gamma", "Dept Gamma", "Desc C", "Qual C", null, LocalDate.now().plusDays(30), null, adminUser));
        bookmarkRepository.save(new Bookmark(otherUser, otherJob));


        mockMvc.perform(get("/api/bookmarks/mybookmarks?page=0&size=5&sort=jobTitle,asc")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].jobTitle").value("Job Alpha")) // Assuming Alpha sorts before Beta
                .andExpect(jsonPath("$.content[1].jobTitle").value("Job Beta"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getUserBookmarks_asUser_noBookmarks() throws Exception {
        mockMvc.perform(get("/api/bookmarks/mybookmarks")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
