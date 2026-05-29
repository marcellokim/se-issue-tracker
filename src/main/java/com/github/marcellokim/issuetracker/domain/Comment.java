package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Comment {

    private final long id;
    private final long issueId;
    private final String commentId;
    private final String writerId;
    private String content;
    private final CommentPurpose purpose;
    private final LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private final User writer;

    private Comment(
            long id,
            long issueId,
            String commentId,
            String writerId,
            User writer,
            String content,
            CommentPurpose purpose,
            LocalDateTime createdDate,
            LocalDateTime updatedDate) {
        this.id = id;
        this.issueId = issueId;
        this.commentId = requireText(commentId, "commentId");
        this.writerId = requireText(writerId, "writerId");
        this.writer = writer;
        this.content = requireText(content, "content");
        this.purpose = Objects.requireNonNull(purpose, "purpose must not be null");
        this.createdDate = Objects.requireNonNull(createdDate, "createdDate must not be null");
        this.updatedDate = Objects.requireNonNull(updatedDate, "updatedDate must not be null");
    }

    public static Comment fromPersistence(
            long id,
            long issueId,
            String writerId,
            String content,
            CommentPurpose purpose,
            LocalDateTime createdDate,
            LocalDateTime updatedDate) {
        return new Comment(id, issueId, Long.toString(id), writerId,
                null, content, purpose, createdDate, updatedDate);
    }

    public static Comment create(
            String commentId,
            String content,
            User writer,
            CommentPurpose purpose,
            LocalDateTime createdDate) {
        Objects.requireNonNull(writer, "writer must not be null");
        return new Comment(0L, 0L, commentId, writer.getLoginId(),
                writer, content, purpose, createdDate, createdDate);
    }

    public static Comment newForIssue(
            long issueId,
            String content,
            User writer,
            CommentPurpose purpose,
            LocalDateTime createdDate) {
        Objects.requireNonNull(writer, "writer must not be null");
        return new Comment(0L, requirePositive(issueId, "issueId"), "NEW-COMMENT", writer.getLoginId(),
                writer, content, purpose, createdDate, createdDate);
    }

    public void changeContent(String newContent, LocalDateTime changedAt) {
        content = requireText(newContent, "content");
        updatedDate = Objects.requireNonNull(changedAt, "changedAt must not be null");
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

    public CommentPurpose purpose() {
        return purpose;
    }

    public LocalDateTime createdDate() {
        return createdDate;
    }

    public LocalDateTime updatedDate() {
        return updatedDate;
    }

    public String getCommentId() {
        return commentId;
    }

    public String getContent() {
        return content;
    }

    public CommentPurpose getPurpose() {
        return purpose;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
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

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
