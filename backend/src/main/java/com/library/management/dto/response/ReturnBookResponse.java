package com.library.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnBookResponse {
    private Long loanId;
    private String bookTitle;
    private LocalDateTime returnDate;
    private Boolean wasOverdue;
    private FineResponse fine;
    private String message;
}


