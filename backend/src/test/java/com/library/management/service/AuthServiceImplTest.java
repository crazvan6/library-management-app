package com.library.management.service;

import com.library.management.dto.request.LoginRequest;
import com.library.management.dto.request.RegisterRequest;
import com.library.management.dto.response.AuthResponse;
import com.library.management.entity.User;
import com.library.management.enums.UserRole;
import com.library.management.exception.DuplicateResourceException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.exception.UnauthorizedException;
import com.library.management.repository.UserRepository;
import com.library.management.security.util.JwtTokenProvider;
import com.library.management.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);
    }

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("student@example.com");
        request.setPassword("Password1!");
        request.setConfirmPassword("Password1!");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setStudentId("S123");

        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(userRepository.existsByStudentId("S123")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(1L);
            return user;
        });
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("student@example.com", response.getEmail());
        assertEquals(UserRole.STUDENT, response.getRole());
        assertEquals("jwt-token", response.getToken());
        assertEquals(86400000L, response.getExpiresIn());
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("student@example.com");
        request.setPassword("Password1!");
        request.setConfirmPassword("Password1!");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setStudentId("S123");

        when(userRepository.existsByEmail("student@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("Password1!");

        User user = User.builder()
                .userId(1L)
                .email(request.getEmail())
                .password("hashed")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.STUDENT)
                .isActive(true)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals(UserRole.STUDENT, response.getRole());
    }

    @Test
    void login_inactiveUser_throws() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("Password1!");

        User inactive = User.builder()
                .userId(1L)
                .email(request.getEmail())
                .password("hashed")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.STUDENT)
                .isActive(false)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(inactive));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void login_invalidCredentials_throws() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("WrongPassword");

        doThrow(new org.springframework.security.authentication.BadCredentialsException("bad"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void login_userNotFound_throws() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("Password1!");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(request));
    }

    @Test
    void validateToken_invalid_throws() {
        when(jwtTokenProvider.validateToken("bad")).thenReturn(false);
        assertThrows(UnauthorizedException.class, () -> authService.validateToken("bad"));
    }
}

