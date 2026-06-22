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
public class FineResponse {
    private Long fineId;
    private Long loanId;
    private Long userId;
    private String userFullName;
    private String bookTitle;
    private String bookAuthor;
    private BigDecimal amount;
    private Integer daysOverdue;
    private FineStatus status;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String processedByName;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


