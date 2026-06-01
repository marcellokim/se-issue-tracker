package com.github.marcellokim.issuetracker.service;

public record CommentActionResult(String commentId, boolean canUpdate, boolean canDelete) {
}
