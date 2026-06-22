package com.library.management.service;

import com.library.management.dto.request.CreateLibrarianRequest;
import com.library.management.dto.request.UpdateProfileRequest;
import com.library.management.entity.Loan;
import com.library.management.entity.User;
import com.library.management.enums.LoanStatus;
import com.library.management.enums.UserRole;
import com.library.management.exception.DuplicateResourceException;
import com.library.management.exception.ForbiddenException;
import com.library.management.exception.InvalidOperationException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.UserRepository;
import com.library.management.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "maxOutstandingFines", new BigDecimal("10.00"));
    }

    @Test
    void getProfile_notFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile(1L));
    }

    @Test
    void updateProfile_duplicateStudentId_throws() {
        User user = baseStudent();
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setStudentId("NEWID");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByStudentId("NEWID")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.updateProfile(1L, request));
    }

    @Test
    void createLibrarian_nonAdmin_throws() {
        CreateLibrarianRequest request = new CreateLibrarianRequest();
        request.setEmail("lib@example.com");
        request.setPassword("Password1!");
        request.setFirstName("Lib");
        request.setLastName("Rarian");

        User creator = baseStudent();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        assertThrows(ForbiddenException.class, () -> userService.createLibrarian(request, 1L));
    }

    @Test
    void createLibrarian_success() {
        CreateLibrarianRequest request = new CreateLibrarianRequest();
        request.setEmail("lib@example.com");
        request.setPassword("Password1!");
        request.setFirstName("Lib");
        request.setLastName("Rarian");

        User admin = baseAdmin();
        User saved = User.builder()
                .userId(5L)
                .email(request.getEmail())
                .firstName("Lib")
                .lastName("Rarian")
                .role(UserRole.LIBRARIAN)
                .isActive(true)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        var response = userService.createLibrarian(request, 99L);

        assertEquals(UserRole.LIBRARIAN, response.getRole());
        assertEquals("lib@example.com", response.getEmail());
    }

    @Test
    void deactivateUser_withActiveLoans_throws() {
        User user = baseStudent();
        user.setLoans(List.of(Loan.builder().status(LoanStatus.ACTIVE).build()));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(InvalidOperationException.class, () -> userService.deactivateUser(1L));
    }

    @Test
    void activateUser_setsActive() {
        User user = baseStudent();
        user.setIsActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.activateUser(1L);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void canUserBorrow_withOutstandingFines_false() {
        doReturn(true).when(userRepository).hasOutstandingFinesAbove(anyLong(), any(BigDecimal.class));

        boolean result = userService.canUserBorrow(1L);

        assertFalse(result);
    }

    private User baseStudent() {
        return User.builder()
                .userId(1L)
                .email("student@example.com")
                .password("hashed")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.STUDENT)
                .studentId("S123")
                .isActive(true)
                .build();
    }

    private User baseAdmin() {
        return User.builder()
                .userId(99L)
                .email("admin@example.com")
                .password("hashed")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();
    }
}


