package com.library.management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReturnBookRequest {
    @NotNull(message = "Loan ID is required")
    private Long loanId;

    @Size(max = 500)
    private String notes;
}


