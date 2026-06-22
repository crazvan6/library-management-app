package com.library.management.dto.response;

import com.library.management.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private Long userId;
    private String email;
    private String fullName;
    private UserRole role;
    private Boolean isActive;
    private long activeLoansCount;
    private BigDecimal totalOutstandingFines;
}


