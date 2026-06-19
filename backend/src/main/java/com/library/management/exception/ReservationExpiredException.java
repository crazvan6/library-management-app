package com.library.management.exception;

public class ReservationExpiredException extends RuntimeException {
    public ReservationExpiredException(String message) {
        super(message);
    }

    public ReservationExpiredException() {
        this("Reservation has expired");
    }
}


