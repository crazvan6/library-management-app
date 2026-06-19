package com.library.management.service;

import com.library.management.dto.request.CreateReservationRequest;
import com.library.management.dto.response.ReservationResponse;

import java.util.List;

public interface ReservationService {
    ReservationResponse createReservation(CreateReservationRequest request, Long userId);

    void cancelReservation(Long reservationId, Long userId);

    ReservationResponse getReservationById(Long reservationId);

    List<ReservationResponse> getMyReservations(Long userId);

    List<ReservationResponse> getPendingReservations(Long userId);

    List<ReservationResponse> getReservationsForBook(Long bookId);

    int getQueuePosition(Long reservationId);

    void expireReservations();
}


