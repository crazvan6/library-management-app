package com.library.management.dto.response;

import com.library.management.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanSummaryResponse {
    private Long loanId;
    private String bookTitle;
    private String bookAuthor;
    private LocalDateTime checkoutDate;
    private LocalDateTime dueDate;
    private LoanStatus status;
    private Boolean isOverdue;
    private Long daysUntilDue;
    private Boolean canBeRenewed;
}


