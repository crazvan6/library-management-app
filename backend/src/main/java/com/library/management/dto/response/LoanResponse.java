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
public class LoanResponse {
    private Long loanId;
    private Long userId;
    private String userFullName;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookIsbn;
    private Long reservationId;
    private LocalDateTime checkoutDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private Integer renewalCount;
    private Boolean isRenewable;
    private Boolean canBeRenewed;
    private LoanStatus status;
    private Boolean isOverdue;
    private Long daysUntilDue;
    private Long daysOverdue;
    private String checkedOutByName;
    private String returnedByName;
    private Boolean hasFine;
    private LocalDateTime createdAt;
}


