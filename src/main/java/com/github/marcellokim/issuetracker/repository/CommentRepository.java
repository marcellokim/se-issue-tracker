package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.Comment;
import java.util.List;
import java.util.Optional;

public interface CommentRepository {

    Optional<Comment> findById(long commentId);

    List<Comment> findByIssueId(long issueId);

    Comment save(Comment comment);

    void deleteById(long commentId);
}
