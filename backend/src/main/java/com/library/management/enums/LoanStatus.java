package com.library.management.enums;

/**
 * Status of a loan lifecycle.
 */
public enum LoanStatus {
    ACTIVE,
    RETURNED,
    OVERDUE;

    /**
     * @return true if loan is active or overdue (still outstanding).
     */
    public boolean isActive() {
        return this == ACTIVE || this == OVERDUE;
    }

    /**
     * @return true when loan is returned.
     */
    public boolean isReturned() {
        return this == RETURNED;
    }

    /**
     * @return true if the loan can be renewed (only ACTIVE).
     */
    public boolean canBeRenewed() {
        return this == ACTIVE;
    }
}

