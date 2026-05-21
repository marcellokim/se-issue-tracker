package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.User;
import java.time.LocalDateTime;

public record CommentResult(
        String commentId,
        String content,
        User writer,
        LocalDateTime createdDate
) {
}
