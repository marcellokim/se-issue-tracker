package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.IssueStateController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.service.CommentActionResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import java.util.List;
import java.util.Objects;

final class IssueDetailPresenter {

    private final IssueController issueController;
    private final IssueStateController issueStateController;
    private final IssueDetailView view;

    IssueDetailPresenter(IssueController issueController, IssueDetailView view) {
        this(issueController, null, view);
    }

    IssueDetailPresenter(
            IssueController issueController,
            IssueStateController issueStateController,
            IssueDetailView view) {
        this.issueController = Objects.requireNonNull(issueController, "issueController");
        this.issueStateController = issueStateController;
        this.view = Objects.requireNonNull(view, "view");
    }

    void loadIssue(long issueId) {
        try {
            IssueDetailResult detail = issueController.viewIssueDetail(issueId);
            view.showDetail(detail, commentActions(issueId));
            view.showMessage(" ", false);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void refreshActions(long issueId) {
        try {
            view.showActions(issueController.viewAvailableActions(issueId));
            view.showMessage(" ", false);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void changeStatus(long issueId, IssueStatus targetStatus, String comment) {
        try {
            requireIssueStateController().changeStatus(issueId, targetStatus, comment);
            loadIssue(issueId);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    private List<IssueCommentActionState> commentActions(long issueId) {
        return issueController.viewCommentActions(issueId).stream()
                .map(IssueDetailPresenter::toCommentActionState)
                .toList();
    }

    private static IssueCommentActionState toCommentActionState(CommentActionResult result) {
        return new IssueCommentActionState(
                result.commentId(),
                result.canUpdate(),
                result.canDelete());
    }

    private IssueStateController requireIssueStateController() {
        if (issueStateController == null) {
            throw new IllegalStateException("Issue state controller is not configured.");
        }
        return issueStateController;
    }
}
