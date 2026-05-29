package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import java.util.List;
import java.util.Optional;

public interface CommentRepository {

    Optional<Comment> findById(long commentId);

    List<Comment> findByIssueId(long issueId);

    Comment save(Comment comment);

    Comment saveCommentAndRecordHistory(Comment comment, IssueHistory history);

    void deleteGeneralById(long issueId, long commentId, String writerLoginId);

    void deleteGeneralByIdAndRecordIssueChange(
            long issueId,
            long commentId,
            String writerLoginId,
            IssueHistory history);
}
