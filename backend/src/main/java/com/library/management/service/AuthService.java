package com.library.management.service;

import com.library.management.dto.request.LoginRequest;
import com.library.management.dto.request.RegisterRequest;
import com.library.management.dto.response.AuthResponse;

public interface AuthService {

    /**
     * Registers a new student user.
     *
     * @param request registration request
     * @return authentication response containing JWT
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user.
     *
     * @param request login request
     * @return authentication response containing JWT
     */
    AuthResponse login(LoginRequest request);

    /**
     * Validates a JWT token.
     *
     * @param token JWT token
     */
    void validateToken(String token);
}


