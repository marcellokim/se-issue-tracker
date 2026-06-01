package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.AssignmentController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.IssueStateController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.service.CommentActionResult;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class IssueDetailPresenter {

    private final IssueController issueController;
    private final IssueStateController issueStateController;
    private final AssignmentController assignmentController;
    private final IssueDetailView view;

    IssueDetailPresenter(IssueController issueController, IssueDetailView view) {
        this(issueController, null, null, view);
    }

    IssueDetailPresenter(
            IssueController issueController,
            IssueStateController issueStateController,
            IssueDetailView view) {
        this(issueController, issueStateController, null, view);
    }

    IssueDetailPresenter(
            IssueController issueController,
            IssueStateController issueStateController,
            AssignmentController assignmentController,
            IssueDetailView view) {
        this.issueController = Objects.requireNonNull(issueController, "issueController");
        this.issueStateController = issueStateController;
        this.assignmentController = assignmentController;
        this.view = Objects.requireNonNull(view, "view");
    }

    void loadIssue(long issueId) {
        try {
            IssueDetailResult detail = issueController.viewIssueDetail(issueId);
            view.showDetail(detail, commentActions(issueId, detail.comments()));
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

    void changeAssignment(long issueId, IssueAssignmentRequest request) {
        try {
            IssueAssignmentRequest requiredRequest = Objects.requireNonNull(request, "request");
            switch (requiredRequest.mode()) {
                case ASSIGN -> requireAssignmentController().assignIssue(
                        issueId,
                        requiredRequest.assigneeId(),
                        requiredRequest.verifierId());
                case REASSIGN_DEV -> requireAssignmentController().reassignIssue(
                        issueId,
                        requiredRequest.assigneeId());
                case CHANGE_TESTER -> requireAssignmentController().changeVerifier(
                        issueId,
                        requiredRequest.verifierId());
            }
            loadIssue(issueId);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void changeComment(long issueId, IssueCommentRequest request) {
        IssueCommentRequest requiredRequest = Objects.requireNonNull(request, "request");
        switch (requiredRequest.mode()) {
            case ADD -> addComment(issueId, requiredRequest.content());
            case UPDATE -> updateComment(issueId, requiredRequest.commentId(), requiredRequest.content());
            case DELETE -> deleteComment(issueId, requiredRequest.commentId());
        }
    }

    void addComment(long issueId, String content) {
        try {
            issueController.addComment(issueId, content);
            loadIssue(issueId);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void updateComment(long issueId, long commentId, String content) {
        try {
            issueController.updateComment(issueId, commentId, content);
            loadIssue(issueId);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void deleteComment(long issueId, long commentId) {
        try {
            issueController.deleteComment(issueId, commentId);
            loadIssue(issueId);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    private List<IssueCommentActionState> commentActions(long issueId, List<CommentResult> comments) {
        Map<String, CommentActionResult> actionsById = commentActionsById(issueId);
        return comments.stream()
                .map(comment -> commentAction(comment, actionsById.get(comment.commentId())))
                .toList();
    }

    private Map<String, CommentActionResult> commentActionsById(long issueId) {
        Map<String, CommentActionResult> actionsById = new LinkedHashMap<>();
        for (CommentActionResult action : issueController.viewCommentActions(issueId)) {
            actionsById.put(action.commentId(), action);
        }
        return actionsById;
    }

    private static IssueCommentActionState commentAction(CommentResult comment, CommentActionResult action) {
        Long commentId = numericCommentId(comment.commentId());
        return new IssueCommentActionState(
                comment.commentId(),
                commentId,
                comment.content(),
                action != null && action.canUpdate(),
                action != null && action.canDelete());
    }

    private static Long numericCommentId(String commentId) {
        try {
            return Long.valueOf(commentId);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private IssueStateController requireIssueStateController() {
        if (issueStateController == null) {
            throw new IllegalStateException("Issue state controller is not configured.");
        }
        return issueStateController;
    }

    private AssignmentController requireAssignmentController() {
        if (assignmentController == null) {
            throw new IllegalStateException("Assignment controller is not configured.");
        }
        return assignmentController;
    }
}
