package com.library.management.enums;

/**
 * Represents supported application roles with convenience helpers for Spring Security.
 */
public enum UserRole {
    STUDENT,
    LIBRARIAN,
    ADMIN;

    /**
     * Returns the authority string expected by Spring Security for this role.
     *
     * @return authority string in the format ROLE_{ROLE_NAME}
     */
    public String getAuthority() {
        return "ROLE_" + name();
    }

    /**
     * Converts a string to {@link UserRole}, ignoring case.
     *
     * @param role role value as string
     * @return matching {@link UserRole}
     * @throws IllegalArgumentException if the value does not match any role
     */
    public static UserRole fromString(String role) {
        return valueOf(role.toUpperCase());
    }
}


