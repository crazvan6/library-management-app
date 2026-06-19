package com.library.management.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException() {
        this("You don't have permission to access this resource");
    }
}


