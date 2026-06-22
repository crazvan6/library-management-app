package com.library.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WaiveFineRequest {
    @NotNull(message = "Fine ID is required")
    private Long fineId;

    @NotBlank(message = "Reason is required")
    @Size(max = 500)
    private String reason;
}


