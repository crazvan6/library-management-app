package com.library.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.management.dto.request.CreateLibrarianRequest;
import com.library.management.dto.request.UpdateProfileRequest;
import com.library.management.dto.response.UserProfileResponse;
import com.library.management.dto.response.UserSummaryResponse;
import com.library.management.entity.User;
import com.library.management.enums.UserRole;
import com.library.management.exception.handler.GlobalExceptionHandler;
import com.library.management.repository.UserRepository;
import com.library.management.security.JwtAuthenticationFilter;
import com.library.management.security.SecurityConfig;
import com.library.management.service.UserService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = "server.servlet.context-path=/")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

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
    void getMyProfile_shouldReturnProfile() throws Exception {
        User user = sampleUser(1L, UserRole.STUDENT);
        UserProfileResponse response = UserProfileResponse.builder()
                .userId(1L)
                .email("student@example.com")
                .fullName("John Doe")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.STUDENT)
                .studentId("S123")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.getProfile(1L)).thenReturn(response);

        mockMvc.perform(get("/v1/users/profile").with(auth(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void updateProfile_shouldReturnUpdatedProfile() throws Exception {
        User user = sampleUser(1L, UserRole.STUDENT);
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setStudentId("S999");

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(1L)
                .email("student@example.com")
                .fullName("Jane Doe")
                .firstName("Jane")
                .lastName("Doe")
                .role(UserRole.STUDENT)
                .studentId("S999")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/v1/users/profile")
                        .with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.studentId").value("S999"));
    }

    @Test
    void getAllUsers_shouldReturnList() throws Exception {
        UserSummaryResponse summary = UserSummaryResponse.builder()
                .userId(1L)
                .email("student@example.com")
                .fullName("John Doe")
                .role(UserRole.STUDENT)
                .isActive(true)
                .activeLoansCount(0)
                .totalOutstandingFines(BigDecimal.ZERO)
                .build();

        when(userService.getAllUsers()).thenReturn(List.of(summary));

        User admin = sampleUser(99L, UserRole.ADMIN);

        mockMvc.perform(get("/v1/users/all").with(auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("student@example.com"));
    }

    @Test
    void createLibrarian_shouldReturnCreated() throws Exception {
        User admin = sampleUser(99L, UserRole.ADMIN);
        CreateLibrarianRequest request = new CreateLibrarianRequest();
        request.setEmail("librarian@example.com");
        request.setPassword("Password1!");
        request.setFirstName("Lib");
        request.setLastName("Rarian");

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(10L)
                .email(request.getEmail())
                .fullName("Lib Rarian")
                .firstName("Lib")
                .lastName("Rarian")
                .role(UserRole.LIBRARIAN)
                .isActive(true)
                .build();

        when(userService.createLibrarian(any(CreateLibrarianRequest.class), eq(99L))).thenReturn(response);

        mockMvc.perform(post("/v1/users/librarian")
                        .with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("LIBRARIAN"))
                .andExpect(jsonPath("$.email").value("librarian@example.com"));
    }

    @Test
    void activateUser_shouldReturnOk() throws Exception {
        Mockito.doNothing().when(userService).activateUser(anyLong());

        User admin = sampleUser(99L, UserRole.ADMIN);

        mockMvc.perform(put("/v1/users/5/activate").with(auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User activated successfully"));
    }

    @Test
    void deactivateUser_shouldReturnOk() throws Exception {
        Mockito.doNothing().when(userService).deactivateUser(anyLong());

        User admin = sampleUser(99L, UserRole.ADMIN);

        mockMvc.perform(put("/v1/users/5/deactivate").with(auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deactivated successfully"));
    }

    private User sampleUser(Long id, UserRole role) {
        return User.builder()
                .userId(id)
                .email("student@example.com")
                .password("Password1!")
                .firstName("John")
                .lastName("Doe")
                .role(role)
                .studentId("S123")
                .isActive(true)
                .build();
    }

    private RequestPostProcessor auth(User user) {
        return user(user);
    }
}

