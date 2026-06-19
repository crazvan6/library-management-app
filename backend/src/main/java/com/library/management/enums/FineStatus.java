package com.library.management.enums;

/**
 * Payment status for fines.
 */
public enum FineStatus {
    PENDING,
    PAID,
    WAIVED;

    /**
     * @return true if fine is still pending.
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * @return true if fine is paid.
     */
    public boolean isPaid() {
        return this == PAID;
    }

    /**
     * @return true if fine is waived.
     */
    public boolean isWaived() {
        return this == WAIVED;
    }

    /**
     * @return true if fine is resolved (paid or waived).
     */
    public boolean isResolved() {
        return this == PAID || this == WAIVED;
    }
}

