package com.library.management.enums;

/**
 * Represents lifecycle status of a book in the catalog.
 */
public enum BookStatus {
    AVAILABLE,
    MAINTENANCE,
    DISCONTINUED;

    /**
     * @return true only when the book can be loaned.
     */
    public boolean isAvailable() {
        return this == AVAILABLE;
    }

    /**
     * @return user-friendly display name.
     */
    public String getDisplayName() {
        return switch (this) {
            case AVAILABLE -> "Available";
            case MAINTENANCE -> "Maintenance";
            case DISCONTINUED -> "Discontinued";
        };
    }
}


