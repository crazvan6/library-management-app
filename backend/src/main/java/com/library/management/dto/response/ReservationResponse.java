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
public class ReservationResponse {
    private Long reservationId;
    private Long userId;
    private String userFullName;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookIsbn;
    private ReservationStatus status;
    private LocalDateTime requestDate;
    private LocalDateTime expiryDate;
    private LocalDateTime completedDate;
    private Long hoursUntilExpiry;
    private Boolean isExpired;
    private Boolean canBeCanceled;
    private Integer queuePosition;
    private LocalDateTime createdAt;
}


