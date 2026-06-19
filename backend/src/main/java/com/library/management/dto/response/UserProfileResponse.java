package com.library.management.dto.response;

import com.library.management.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long userId;
    private String email;
    private String fullName;
    private String firstName;
    private String lastName;
    private UserRole role;
    private String studentId;
    private Boolean isActive;
    private LocalDateTime createdAt;
}


