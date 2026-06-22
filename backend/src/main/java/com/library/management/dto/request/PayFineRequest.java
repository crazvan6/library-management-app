package com.library.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PayFineRequest {
    @NotNull(message = "Fine ID is required")
    private Long fineId;

    @NotBlank(message = "Payment method is required")
    @Size(max = 50)
    private String paymentMethod;
}


