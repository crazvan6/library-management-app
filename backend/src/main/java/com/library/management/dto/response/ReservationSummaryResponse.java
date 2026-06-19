package com.library.management.dto.response;

import com.library.management.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationSummaryResponse {
    private Long reservationId;
    private String bookTitle;
    private String bookAuthor;
    private ReservationStatus status;
    private LocalDateTime expiryDate;
    private Long hoursUntilExpiry;
    private Boolean canBeCanceled;
}


