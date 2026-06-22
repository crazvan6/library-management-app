package com.library.management.exception;

public class UserNotEligibleException extends RuntimeException {
    public UserNotEligibleException(String message) {
        super(message);
    }

    public UserNotEligibleException() {
        this("User is not eligible for this operation");
    }
}


