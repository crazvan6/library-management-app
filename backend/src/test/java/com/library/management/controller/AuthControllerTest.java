package com.library.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.management.dto.request.LoginRequest;
import com.library.management.dto.request.RegisterRequest;
import com.library.management.dto.response.AuthResponse;
import com.library.management.enums.UserRole;
import com.library.management.exception.handler.GlobalExceptionHandler;
import com.library.management.repository.UserRepository;
import com.library.management.security.JwtAuthenticationFilter;
import com.library.management.security.SecurityConfig;
import com.library.management.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = "server.servlet.context-path=/")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setupFilter() throws ServletException, IOException {
        Mockito.doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void register_shouldReturnCreated() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("student@example.com");
        request.setPassword("Password1!");
        request.setConfirmPassword("Password1!");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setStudentId("S123");

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .userId(1L)
                .email(request.getEmail())
                .fullName("John Doe")
                .role(UserRole.STUDENT)
                .expiresIn(86400000L)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(request.getEmail()))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void login_shouldReturnOk() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("Password1!");

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .userId(1L)
                .email(request.getEmail())
                .fullName("John Doe")
                .role(UserRole.STUDENT)
                .expiresIn(86400000L)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(request.getEmail()))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void validateToken_shouldReturnOk() throws Exception {
        doNothing().when(authService).validateToken("jwt-token");

        mockMvc.perform(get("/v1/auth/validate")
                        .header("Authorization", "Bearer jwt-token")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token is valid"));

        Mockito.verify(authService).validateToken(eq("jwt-token"));
    }

    @Test
    void validateToken_missingHeader_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/v1/auth/validate").with(user("tester")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No token provided"));
    }
}

