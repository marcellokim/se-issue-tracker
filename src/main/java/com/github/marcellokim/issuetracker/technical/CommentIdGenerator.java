package com.github.marcellokim.issuetracker.technical;

import com.github.marcellokim.issuetracker.service.CommentIdProvider;
import java.util.UUID;

public final class CommentIdGenerator implements CommentIdProvider {

    private static final String COMMENT_ID_PREFIX = "COMMENT-";

    @Override
    public String nextCommentId() {
        return COMMENT_ID_PREFIX + UUID.randomUUID();
    }
}