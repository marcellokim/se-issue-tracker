package com.github.marcellokim.issuetracker.service;

import java.util.UUID;

final class CommentIdGenerator {

    private static final String COMMENT_ID_PREFIX = "COMMENT-";

    private CommentIdGenerator() {
    }

    static String nextCommentId() {
        return COMMENT_ID_PREFIX + UUID.randomUUID();
    }
}
