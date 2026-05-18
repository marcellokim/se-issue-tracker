package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record Comment(
        long id,
        long issueId,
        String writerId,
        String content,
        LocalDateTime createdDate) {

    public Comment {
        Objects.requireNonNull(writerId, "writerId");
        Objects.requireNonNull(content, "content");
    }
}
