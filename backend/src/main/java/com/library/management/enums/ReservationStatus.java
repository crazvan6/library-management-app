package com.library.management.enums;

/**
 * Status for reservation lifecycle.
 */
public enum ReservationStatus {
    PENDING,
    COMPLETED,
    CANCELED,
    EXPIRED;

    /**
     * @return true when reservation is active and can still be acted upon.
     */
    public boolean isActive() {
        return this == PENDING;
    }

    /**
     * @return true when reservation reached a final state.
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELED || this == EXPIRED;
    }

    /**
     * @return true if reservation can be canceled by user.
     */
    public boolean canBeCanceled() {
        return this == PENDING;
    }
}

