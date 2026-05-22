package com.github.marcellokim.issuetracker.config;

public record DatabaseConnectionSummary(
        String url,
        String user,
        String currentSchema,
        String containerName
) {
}
