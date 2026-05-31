package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.AssignmentController;
import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.IssueStateController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.service.CommentActionResult;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class IssueDetailPresenter {

    private static final String REQUEST_PARAM = "request";

    private final IssueController issueController;
    private final IssueStateController issueStateController;
    private final AssignmentController assignmentController;
    private final DeletedIssueController deletedIssueController;
    private final IssueDetailView view;

    IssueDetailPresenter(IssueController issueController, IssueDetailView view) {
        this(issueController, null, null, null, view);
    }

    IssueDetailPresenter(
            IssueController issueController,
            IssueStateController issueStateController,
            IssueDetailView view) {
        this(issueController, issueStateController, null, null, view);
    }

    IssueDetailPresenter(
            IssueController issueController,
            IssueStateController issueStateController,
            AssignmentController assignmentController,
            IssueDetailView view) {
        this(issueController, issueStateController, assignmentController, null, view);
    }

    IssueDetailPresenter(
            IssueController issueController,
            IssueStateController issueStateController,
            AssignmentController assignmentController,
            DeletedIssueController deletedIssueController,
            IssueDetailView view) {
        this.issueController = Objects.requireNonNull(issueController, "issueController");
        this.issueStateController = issueStateController;
        this.assignmentController = assignmentController;
        this.deletedIssueController = deletedIssueController;
        this.view = Objects.requireNonNull(view, "view");
    }

    void loadIssue(long issueId) {
        try {
            IssueDetailResult detail = issueController.viewIssueDetail(issueId);
            List<DependencyResult> projectDependencies =
                    issueController.viewProjectDependencies(detail.projectId());
            view.showDetail(detail, commentActions(issueId, detail.comments()), projectDependencies);
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
            IssueAssignmentRequest requiredRequest = Objects.requireNonNull(request, REQUEST_PARAM);
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

    void updateIssue(long issueId, IssueEditRequest request) {
        try {
            IssueEditRequest requiredRequest = requireEditRequest(request, IssueEditMode.UPDATE);
            issueController.updateIssue(issueId, requiredRequest.title(), requiredRequest.description());
            loadIssue(issueId);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void changePriority(long issueId, IssueEditRequest request) {
        try {
            IssueEditRequest requiredRequest = requireEditRequest(request, IssueEditMode.CHANGE_PRIORITY);
            issueController.changePriority(issueId, requiredRequest.priority());
            loadIssue(issueId);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    boolean deleteIssue(long issueId, IssueEditRequest request) {
        try {
            IssueEditRequest requiredRequest = requireEditRequest(request, IssueEditMode.SOFT_DELETE);
            requireDeletedIssueController().deleteIssue(issueId, requiredRequest.comment());
            view.showMessage(" ", false);
            return true;
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
            return false;
        }
    }

    void changeComment(long issueId, IssueCommentRequest request) {
        IssueCommentRequest requiredRequest = Objects.requireNonNull(request, REQUEST_PARAM);
        switch (requiredRequest.mode()) {
            case ADD -> addComment(issueId, requiredRequest.content());
            case UPDATE -> updateComment(issueId, requiredRequest.commentId(), requiredRequest.content());
            case DELETE -> deleteComment(issueId, requiredRequest.commentId());
        }
    }

    void changeDependency(long issueId, IssueDependencyRequest request) {
        IssueDependencyRequest requiredRequest = Objects.requireNonNull(request, REQUEST_PARAM);
        switch (requiredRequest.mode()) {
            case ADD -> addDependency(
                    issueId,
                    requiredRequest.blockingIssueId(),
                    requiredRequest.blockedIssueId());
            case REMOVE -> removeDependency(
                    issueId,
                    requiredRequest.blockingIssueId(),
                    requiredRequest.blockedIssueId());
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

    void addDependency(long issueId, long blockingIssueId, long blockedIssueId) {
        try {
            issueController.addDependency(blockingIssueId, blockedIssueId);
            loadIssue(issueId);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void removeDependency(long issueId, long blockingIssueId, long blockedIssueId) {
        try {
            issueController.removeDependency(blockingIssueId, blockedIssueId);
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

    private static IssueEditRequest requireEditRequest(IssueEditRequest request, IssueEditMode mode) {
        IssueEditRequest requiredRequest = Objects.requireNonNull(request, REQUEST_PARAM);
        if (requiredRequest.mode() != mode) {
            throw new IllegalArgumentException("Unexpected issue edit request mode.");
        }
        return requiredRequest;
    }

    private DeletedIssueController requireDeletedIssueController() {
        if (deletedIssueController == null) {
            throw new IllegalStateException("Deleted issue controller is not configured.");
        }
        return deletedIssueController;
    }
}
