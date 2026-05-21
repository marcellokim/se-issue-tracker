package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Comment {

    private final long id;
    private final long issueId;
    private final String commentId;
    private final String writerId;
    private final String content;
    private final CommentPurpose purpose;
    private final LocalDateTime createdDate;
    private final User writer;

    public static Comment fromPersistence(
            long id,
            long issueId,
            String writerId,
            String content,
            LocalDateTime createdDate
    ) {
        return fromPersistence(id, issueId, writerId, content, CommentPurpose.GENERAL, createdDate);
    }

    public static Comment fromPersistence(
            long id,
            long issueId,
            String writerId,
            String content,
            CommentPurpose purpose,
            LocalDateTime createdDate
    ) {
        return new Comment(id, issueId, writerId, content, purpose, createdDate);
    }

    private Comment(
            long id,
            long issueId,
            String writerId,
            String content,
            CommentPurpose purpose,
            LocalDateTime createdDate
    ) {
        this.id = id;
        this.issueId = issueId;
        this.commentId = Long.toString(id);
        this.writerId = requireText(writerId, "writerId");
        this.content = requireText(content, "content");
        this.purpose = Objects.requireNonNull(purpose, "purpose must not be null");
        this.createdDate = Objects.requireNonNull(createdDate, "createdDate must not be null");
        this.writer = null;
    }

    private Comment(String commentId, String content, User writer, CommentPurpose purpose, LocalDateTime createdDate) {
        this.id = 0L;
        this.issueId = 0L;
        this.commentId = requireText(commentId, "commentId");
        this.writer = Objects.requireNonNull(writer, "writer must not be null");
        this.writerId = writer.getLoginId();
        this.content = requireText(content, "content");
        this.purpose = Objects.requireNonNull(purpose, "purpose must not be null");
        this.createdDate = Objects.requireNonNull(createdDate, "createdDate must not be null");
    }

    public static Comment create(String commentId, String content, User writer, LocalDateTime createdDate) {
        return create(commentId, content, writer, CommentPurpose.GENERAL, createdDate);
    }

    public static Comment create(
            String commentId,
            String content,
            User writer,
            CommentPurpose purpose,
            LocalDateTime createdDate
    ) {
        return new Comment(commentId, content, writer, purpose, createdDate);
    }

    // --- getters ---
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
