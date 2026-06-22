package com.library.management.service.impl;

import com.library.management.dto.request.CreateLibrarianRequest;
import com.library.management.dto.request.UpdateProfileRequest;
import com.library.management.dto.response.UserProfileResponse;
import com.library.management.dto.response.UserSummaryResponse;
import com.library.management.entity.User;
import com.library.management.enums.LoanStatus;
import com.library.management.enums.UserRole;
import com.library.management.exception.DuplicateResourceException;
import com.library.management.exception.ForbiddenException;
import com.library.management.exception.InvalidOperationException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.UserRepository;
import com.library.management.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${library.fine.max-outstanding}")
    private BigDecimal maxOutstandingFines;

    @Override
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return toUserProfileResponse(user);
    }

    @Override
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.isStudent() && StringUtils.hasText(request.getStudentId())
                && !request.getStudentId().equals(user.getStudentId())
                && userRepository.existsByStudentId(request.getStudentId())) {
            throw new DuplicateResourceException("User", "studentId", request.getStudentId());
        }

        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName());
        }
        if (user.isStudent() && StringUtils.hasText(request.getStudentId())) {
            user.setStudentId(request.getStudentId());
        }

        userRepository.save(user);
        log.info("User profile updated: {}", userId);
        return toUserProfileResponse(user);
    }

    @Override
    public List<UserSummaryResponse> getAllUsers() {
        return userRepository.findAllUsersWithSummary();
    }

    @Override
    public UserProfileResponse createLibrarian(CreateLibrarianRequest request, Long createdBy) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User creator = userRepository.findById(createdBy)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", createdBy));
        if (!creator.isAdmin()) {
            throw new ForbiddenException("Only admins can create librarians");
        }

        User librarian = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(UserRole.LIBRARIAN)
                .isActive(true)
                .build();

        userRepository.save(librarian);
        log.info("Librarian created by admin {}: {}", createdBy, librarian.getEmail());
        return toUserProfileResponse(librarian);
    }

    @Override
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setIsActive(true);
        userRepository.save(user);
        log.info("User activated: {}", userId);
    }

    @Override
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        boolean hasActiveLoans = user.getLoans().stream()
                .anyMatch(loan -> LoanStatus.ACTIVE.equals(loan.getStatus()));
        if (hasActiveLoans) {
            throw new InvalidOperationException("Cannot deactivate user with active loans");
        }

        user.setIsActive(false);
        userRepository.save(user);
        log.info("User deactivated: {}", userId);
    }

    @Override
    public boolean canUserBorrow(Long userId) {
        boolean hasOutstanding = userRepository.hasOutstandingFinesAbove(userId, maxOutstandingFines);
        return !hasOutstanding;
    }

    private UserProfileResponse toUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .studentId(user.getStudentId())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}


