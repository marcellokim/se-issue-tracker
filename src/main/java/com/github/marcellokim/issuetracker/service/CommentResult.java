package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.User;
import java.time.LocalDateTime;

public record CommentResult(
        String commentId,
        String content,
        CommentPurpose purpose,
        User writer,
        LocalDateTime createdDate,
        LocalDateTime updatedDate
) {
}
