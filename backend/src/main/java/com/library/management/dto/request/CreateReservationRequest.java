package com.library.management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReservationRequest {
    @NotNull(message = "Book ID is required")
    private Long bookId;
}


