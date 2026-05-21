package com.github.marcellokim.issuetracker.persistence;

import java.util.Map;
import java.util.Objects;

public record DatabaseEnvironment(String url, String user, String password) {

    public static final String URL_VARIABLE = "ITS_DB_URL";
    public static final String USER_VARIABLE = "ITS_DB_USER";
    public static final String PASSWORD_VARIABLE = "ITS_DB_PASSWORD";
    public static final String MISSING_MESSAGE =
            "Oracle environment is missing. Set ITS_DB_URL, ITS_DB_USER, ITS_DB_PASSWORD.";

    public DatabaseEnvironment {
        url = requireText(url, "url");
        user = requireText(user, "user");
        password = requireText(password, "password");
    }

    public static boolean isSystemConfigured() {
        return isConfigured(System.getenv());
    }

    public static boolean isConfigured(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");
        return hasText(environment.get(URL_VARIABLE))
                && hasText(environment.get(USER_VARIABLE))
                && hasText(environment.get(PASSWORD_VARIABLE));
    }

    public static DatabaseEnvironment fromSystem() {
        return requireConfigured(System.getenv());
    }

    public static DatabaseEnvironment requireConfigured(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");
        if (!isConfigured(environment)) {
            throw new IllegalStateException(MISSING_MESSAGE);
        }
        /*
         * Oracle application 환경 해석을 하나의 value object에 모아 CLI, JavaFX startup,
         * JDBC provider 생성이 같은 준비 상태 규칙을 공유하게 한다.
         */
        return new DatabaseEnvironment(
                environment.get(URL_VARIABLE),
                environment.get(USER_VARIABLE),
                environment.get(PASSWORD_VARIABLE));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String requireText(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
