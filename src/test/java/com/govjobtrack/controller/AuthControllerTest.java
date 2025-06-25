package com.govjobtrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govjobtrack.model.Role;
import com.govjobtrack.model.RoleEntity;
import com.govjobtrack.model.User;
import com.govjobtrack.payload.request.LoginRequest;
import com.govjobtrack.payload.request.SignupRequest;
import com.govjobtrack.payload.response.JwtResponse;
import com.govjobtrack.payload.response.MessageResponse;
import com.govjobtrack.repository.RoleRepository;
import com.govjobtrack.repository.UserRepository;
import com.govjobtrack.service.AuthService; // We might mock this or use the real one
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean; // Option to mock service
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // For full integration test of AuthController + AuthService + Repositories
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // OR if we want to mock the service layer:
    // @MockBean
    // private AuthService authService;


    @BeforeEach
    void setUp() {
        // Clean up database before each test to ensure isolation
        userRepository.deleteAll();
        roleRepository.deleteAll(); // Be careful if DataInitializer runs and you delete here

        // Ensure roles are present (similar to DataInitializer but for test context)
        if (roleRepository.findByName(Role.ROLE_USER).isEmpty()) {
            roleRepository.save(new RoleEntity(Role.ROLE_USER));
        }
        if (roleRepository.findByName(Role.ROLE_ADMIN).isEmpty()) {
            roleRepository.save(new RoleEntity(Role.ROLE_ADMIN));
        }
    }

    @Test
    void registerUser_success() throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setFirstName("Test");
        signupRequest.setLastName("User");
        signupRequest.setEmail("test.user@example.com");
        signupRequest.setPassword("password123");
        Set<String> roles = new HashSet<>();
        roles.add("user");
        signupRequest.setRole(roles);

        // If mocking AuthService:
        // when(authService.registerUser(any(SignupRequest.class)))
        //        .thenReturn(ResponseEntity.ok(new MessageResponse("User registered successfully!")));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));

        // Verify user is actually in DB (if not mocking service)
        assertThat(userRepository.existsByEmail("test.user@example.com")).isTrue();
        User savedUser = userRepository.findByEmail("test.user@example.com").get();
        assertThat(savedUser.getFirstName()).isEqualTo("Test");
        assertThat(savedUser.getRoles().stream().anyMatch(role -> role.getName().equals(Role.ROLE_USER))).isTrue();

    }

    @Test
    void registerUser_emailAlreadyExists() throws Exception {
        // Create an existing user
        User existingUser = new User("Existing", "User", "existing.user@example.com", passwordEncoder.encode("password123"));
        RoleEntity userRole = roleRepository.findByName(Role.ROLE_USER).get();
        existingUser.setRoles(Set.of(userRole));
        userRepository.save(existingUser);

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setFirstName("New");
        signupRequest.setLastName("User");
        signupRequest.setEmail("existing.user@example.com"); // Same email
        signupRequest.setPassword("newpassword123");

        // If mocking AuthService:
        // when(authService.registerUser(any(SignupRequest.class)))
        //        .thenReturn(ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!")));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Email is already in use!"));
    }

    @Test
    void registerUser_invalidRequest_missingEmail() throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setFirstName("Test");
        // Email is missing, password missing - should trigger @Valid constraints
        signupRequest.setPassword("short");


        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest()); // Spring validation should trigger a 400
                // We could also check for specific error messages if ValidationConfiguration is set up to return them in a structured way.
    }


    @Test
    void authenticateUser_success() throws Exception {
        // Setup user
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setFirstName("Login");
        signupRequest.setLastName("User");
        signupRequest.setEmail("login.user@example.com");
        signupRequest.setPassword("password123");
        Set<String> roles = new HashSet<>();
        roles.add("user");
        signupRequest.setRole(roles);

        // Register user directly via service or repository for setup
        User user = new User(signupRequest.getFirstName(), signupRequest.getLastName(), signupRequest.getEmail(), passwordEncoder.encode(signupRequest.getPassword()));
        RoleEntity userRole = roleRepository.findByName(Role.ROLE_USER).get();
        user.setRoles(Set.of(userRole));
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login.user@example.com");
        loginRequest.setPassword("password123");

        // If mocking AuthService:
        // JwtResponse jwtResponse = new JwtResponse("testtoken", 1L, "login.user@example.com", "Login", "User", List.of("ROLE_USER"));
        // when(authService.authenticateUser(any(LoginRequest.class)))
        //        .thenReturn(ResponseEntity.ok(jwtResponse));

        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.email").value("login.user@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER")) // Based on UserDetailsImpl conversion
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        JwtResponse jwtResponse = objectMapper.readValue(responseString, JwtResponse.class);
        assertThat(jwtResponse.getFirstName()).isEqualTo("Login");

    }

    @Test
    void authenticateUser_invalidCredentials_wrongPassword() throws Exception {
        // Setup user
        User user = new User("Login", "Fail", "login.fail@example.com", passwordEncoder.encode("correctpassword"));
        RoleEntity userRole = roleRepository.findByName(Role.ROLE_USER).get();
        user.setRoles(Set.of(userRole));
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login.fail@example.com");
        loginRequest.setPassword("wrongpassword"); // Incorrect password

        // Auth manager should throw BadCredentialsException, handled by AuthEntryPointJwt if not caught earlier
        // Or if caught by a global exception handler.
        // Spring Security's default behavior for bad credentials with formLogin is a redirect.
        // With JWT and custom auth, AuthenticationManager throws an exception.
        // Our AuthEntryPointJwt should then kick in if the request is unauthenticated due to filter chain.
        // For /api/auth/signin, the AuthenticationManager itself throws, leading to 401 if not handled specifically
        // by the controller/service to return a custom message.
        // Our AuthServiceImpl currently lets AuthenticationException propagate, which Spring's ExceptionTranslationFilter
        // would pass to AuthEntryPointJwt.

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Bad credentials")); // DaoAuthenticationProvider typically throws BadCredentialsException for wrong password
    }

    @Test
    void authenticateUser_userNotFound() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent.user@example.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Bad credentials")); // DaoAuthenticationProvider also throws BadCredentials for user not found to prevent username enumeration
    }
}
