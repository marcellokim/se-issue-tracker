package com.github.marcellokim.issuetracker.domain;

public record ValidationResult(
        boolean valid,
        String message
) {

    public static ValidationResult ok() {
        return new ValidationResult(true, "");
    }

    public static ValidationResult failure(String message) {
        return new ValidationResult(false, message);
    }
}
