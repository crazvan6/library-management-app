package com.library.management.validator;

import com.library.management.exception.InvalidOperationException;

/**
 * Utility validator to ensure password and confirmation match.
 */
public final class PasswordMatchValidator {

    private PasswordMatchValidator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Validates that two password values match.
     *
     * @param password        raw password
     * @param confirmPassword confirmation
     */
    public static void validatePasswordMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            throw new InvalidOperationException("Passwords do not match");
        }
    }
}


