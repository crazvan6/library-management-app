package com.library.management.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-\\[\\]{}|;:,.<>?].*");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.length() < 8) {
            return false;
        }
        return UPPERCASE_PATTERN.matcher(value).matches()
                && LOWERCASE_PATTERN.matcher(value).matches()
                && DIGIT_PATTERN.matcher(value).matches()
                && SPECIAL_PATTERN.matcher(value).matches();
    }
}

