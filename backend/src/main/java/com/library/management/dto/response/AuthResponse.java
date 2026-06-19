package com.library.management.dto.response;

import com.library.management.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    @Builder.Default
    private String type = "Bearer";
    private String token;
    private Long userId;
    private String email;
    private String fullName;
    private UserRole role;
    private Long expiresIn;
}


