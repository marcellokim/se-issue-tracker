package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Comment {

    private final long id;
    private final long issueId;
    private final String commentId;
    private final String writerId;
    private final String content;
    private final LocalDateTime createdDate;
    private final User writer;

    public Comment(long id, long issueId, String writerId, String content, LocalDateTime createdDate) {
        this.id = id;
        this.issueId = issueId;
        this.commentId = Long.toString(id);
        this.writerId = requireText(writerId, "writerId");
        this.content = requireText(content, "content");
        this.createdDate = createdDate;
        this.writer = null;
    }

    private Comment(String commentId, String content, User writer, LocalDateTime createdDate) {
        this.id = 0L;
        this.issueId = 0L;
        this.commentId = requireText(commentId, "commentId");
        this.writer = Objects.requireNonNull(writer, "writer must not be null");
        this.writerId = writer.loginId();
        this.content = requireText(content, "content");
        this.createdDate = Objects.requireNonNull(createdDate, "createdDate must not be null");
    }

    public static Comment create(String commentId, String content, User writer, LocalDateTime createdDate) {
        return new Comment(commentId, content, writer, createdDate);
    }

    public long id() {
        return id;
    }

    public long issueId() {
        return issueId;
    }

    public String writerId() {
        return writerId;
    }

    public String content() {
        return content;
    }

    public LocalDateTime createdDate() {
        return createdDate;
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
