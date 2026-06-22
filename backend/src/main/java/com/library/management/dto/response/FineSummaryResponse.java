package com.library.management.dto.response;

import com.library.management.enums.FineStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FineSummaryResponse {
    private Long fineId;
    private String bookTitle;
    private BigDecimal amount;
    private Integer daysOverdue;
    private FineStatus status;
    private LocalDateTime createdAt;
}


