package com.library.management.mapper;

import com.library.management.dto.request.CreateReservationRequest;
import com.library.management.dto.response.ReservationResponse;
import com.library.management.dto.response.ReservationSummaryResponse;
import com.library.management.entity.Book;
import com.library.management.entity.Reservation;
import com.library.management.entity.User;
import com.library.management.enums.ReservationStatus;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public ReservationResponse toReservationResponse(Reservation reservation) {
        if (reservation == null) {
            return null;
        }
        return ReservationResponse.builder()
                .reservationId(reservation.getReservationId())
                .userId(reservation.getUser().getUserId())
                .userFullName(reservation.getUser().getFullName())
                .bookId(reservation.getBook().getBookId())
                .bookTitle(reservation.getBook().getTitle())
                .bookAuthor(reservation.getBook().getAuthor())
                .bookIsbn(reservation.getBook().getIsbn())
                .status(reservation.getStatus())
                .requestDate(reservation.getRequestDate())
                .expiryDate(reservation.getExpiryDate())
                .completedDate(reservation.getCompletedDate())
                .hoursUntilExpiry(reservation.getHoursUntilExpiry())
                .isExpired(reservation.isExpired())
                .canBeCanceled(reservation.canBeCanceled())
                .createdAt(reservation.getCreatedAt())
                .build();
    }

    public ReservationSummaryResponse toReservationSummaryResponse(Reservation reservation) {
        return ReservationSummaryResponse.builder()
                .reservationId(reservation.getReservationId())
                .bookTitle(reservation.getBook().getTitle())
                .bookAuthor(reservation.getBook().getAuthor())
                .status(reservation.getStatus())
                .expiryDate(reservation.getExpiryDate())
                .hoursUntilExpiry(reservation.getHoursUntilExpiry())
                .canBeCanceled(reservation.canBeCanceled())
                .build();
    }

    public Reservation toEntity(CreateReservationRequest request, User user, Book book) {
        return Reservation.builder()
                .user(user)
                .book(book)
                .status(ReservationStatus.PENDING)
                .requestDate(java.time.LocalDateTime.now())
                .build();
    }
}


