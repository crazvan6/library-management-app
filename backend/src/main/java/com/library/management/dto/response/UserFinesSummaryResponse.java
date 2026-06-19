package com.library.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFinesSummaryResponse {
    private Long userId;
    private String userFullName;
    private BigDecimal totalOutstandingFines;
    private Integer pendingFinesCount;
    private Integer paidFinesCount;
    private Integer waivedFinesCount;
    private Boolean canBorrow;
    private List<FineSummaryResponse> fines;
}


