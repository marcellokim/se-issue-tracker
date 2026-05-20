package com.github.marcellokim.issuetracker.domain;

import java.util.Objects;

public final class ValidationResult {

    private final boolean valid;
    private final String message;

    public static ValidationResult ok() {
        return new ValidationResult(true, "");
    }

    public static ValidationResult failure(String message) {
        return new ValidationResult(false, message);
    }

    private ValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public boolean valid() {
        return valid;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ValidationResult that)) {
            return false;
        }
        return valid == that.valid && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valid, message);
    }

    @Override
    public String toString() {
        return "ValidationResult[valid=" + valid + ", message=" + message + "]";
    }
}
