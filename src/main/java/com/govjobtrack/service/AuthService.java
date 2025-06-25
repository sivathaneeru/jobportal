package com.govjobtrack.service;

import com.govjobtrack.payload.request.LoginRequest;
import com.govjobtrack.payload.request.SignupRequest;
import com.govjobtrack.payload.response.JwtResponse;
import org.springframework.http.ResponseEntity; // For more flexible response handling

public interface AuthService {
    ResponseEntity<?> registerUser(SignupRequest signupRequest);
    ResponseEntity<?> authenticateUser(LoginRequest loginRequest);
}
