package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Comment {

    private final String commentId;
    private final String content;
    private final LocalDateTime createdDate;
    private final User writer;

    private Comment(String commentId, String content, User writer, LocalDateTime createdDate) {
        this.commentId = requireText(commentId, "commentId");
        this.content = requireText(content, "content");
        this.writer = Objects.requireNonNull(writer, "writer must not be null");
        this.createdDate = Objects.requireNonNull(createdDate, "createdDate must not be null");
    }

    public static Comment create(String commentId, String content, User writer, LocalDateTime createdDate) {
        return new Comment(commentId, content, writer, createdDate);
    }

    public String getCommentId() {
        return commentId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public User getWriter() {
        return writer;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
