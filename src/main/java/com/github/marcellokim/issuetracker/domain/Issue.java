package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Issue {

    private static final String CREATED_PREVIOUS_VALUE = null;
    private static final String COMMENT_FIELD = "comment";
    private static final String CHANGED_BY_REQUIRED = "changedBy must not be null";
    private static final String CHANGED_DATE_REQUIRED = "changedDate must not be null";

    private final String issueId;
    private String title;
    private String description;
    private final LocalDateTime reportedDate;
    private Priority priority = Priority.MAJOR;
    private IssueStatus status = IssueStatus.NEW;
    private final User reporter;
    private User assignee;
    private User verifier;
    private User fixer;
    private User resolver;
    private final List<Comment> comments = new ArrayList<>();
    private final List<IssueHistory> histories = new ArrayList<>();
    private final List<IssueDependency> blockingDependencies = new ArrayList<>();
    private final List<IssueDependency> blockedByDependencies = new ArrayList<>();

    private Issue(
            String issueId,
            String title,
            String description,
            Priority priority,
            User reporter,
            LocalDateTime reportedDate
    ) {
        this.issueId = requireText(issueId, "issueId");
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.priority = priority == null ? Priority.MAJOR : priority;
        this.reporter = Objects.requireNonNull(reporter, "reporter must not be null");
        this.reportedDate = Objects.requireNonNull(reportedDate, "reportedDate must not be null");
        recordHistory(ActionType.CREATED, CREATED_PREVIOUS_VALUE, IssueStatus.NEW.name(), "Issue created", reporter, reportedDate);
    }

    public static Issue create(
            String issueId,
            String title,
            String description,
            Priority priority,
            User reporter,
            LocalDateTime reportedDate
    ) {
        return new Issue(issueId, title, description, priority, reporter, reportedDate);
    }

    public String getIssueId() {
        return issueId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getReportedDate() {
        return reportedDate;
    }

    public Priority getPriority() {
        return priority;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public User getReporter() {
        return reporter;
    }

    public User getAssignee() {
        return assignee;
    }

    public User getVerifier() {
        return verifier;
    }

    public User getFixer() {
        return fixer;
    }

    public User getResolver() {
        return resolver;
    }

    public List<Comment> getComments() {
        return Collections.unmodifiableList(comments);
    }

    public List<IssueHistory> getHistories() {
        return Collections.unmodifiableList(histories);
    }

    public List<IssueDependency> getBlockingDependencies() {
        return Collections.unmodifiableList(blockingDependencies);
    }

    public List<IssueDependency> getBlockedByDependencies() {
        return Collections.unmodifiableList(blockedByDependencies);
    }

    public void assignFromNew(User assignee, User verifier, User changedBy, LocalDateTime changedDate) {
        requireStatus(IssueStatus.NEW);
        assign(assignee, verifier, changedBy, changedDate, "Issue assigned from NEW");
    }

    public void assignReopened(User assignee, User verifier, User changedBy, LocalDateTime changedDate) {
        requireStatus(IssueStatus.REOPENED);
        assign(assignee, verifier, changedBy, changedDate, "Issue assigned from REOPENED");
    }

    public void reassignAssignee(User assignee, User changedBy, LocalDateTime changedDate) {
        requireStatus(IssueStatus.ASSIGNED);
        requireRole(assignee, Role.DEV, "assignee");
        requireRole(changedBy, Role.PL, "changedBy");
        Objects.requireNonNull(changedDate, CHANGED_DATE_REQUIRED);
        if (sameUser(this.assignee, assignee)) {
            throw new IllegalArgumentException("assignee must be different from current assignee");
        }

        var previousAssignee = this.assignee;
        this.assignee = assignee;
        recordHistory(
                ActionType.ASSIGNMENT_CHANGED,
                previousAssignee == null ? null : previousAssignee.getUserId(),
                assignee.getUserId(),
                "Assignee changed",
                changedBy,
                changedDate
        );
    }

    public void changeVerifier(User verifier, User changedBy, LocalDateTime changedDate) {
        requireStatus(IssueStatus.FIXED);
        requireRole(verifier, Role.TESTER, "verifier");
        requireRole(changedBy, Role.PL, "changedBy");
        Objects.requireNonNull(changedDate, CHANGED_DATE_REQUIRED);
        if (sameUser(this.verifier, verifier)) {
            throw new IllegalArgumentException("verifier must be different from current verifier");
        }

        var previousVerifier = this.verifier;
        this.verifier = verifier;
        recordHistory(
                ActionType.ASSIGNMENT_CHANGED,
                previousVerifier == null ? null : previousVerifier.getUserId(),
                verifier.getUserId(),
                "Verifier changed",
                changedBy,
                changedDate
        );
    }

    public void markFixed(User fixer, String comment, LocalDateTime changedDate) {
        requireStatus(IssueStatus.ASSIGNED);
        requireRole(fixer, Role.DEV, "fixer");
        requireCurrentParticipant(fixer, assignee, "fixer must be current assignee");
        var requiredComment = requireText(comment, COMMENT_FIELD);

        this.fixer = fixer;
        changeStatusTo(IssueStatus.FIXED, requiredComment, fixer, changedDate);
    }

    public void resolve(User resolver, String comment, LocalDateTime changedDate) {
        requireStatus(IssueStatus.FIXED);
        requireRole(resolver, Role.TESTER, "resolver");
        requireCurrentParticipant(resolver, verifier, "resolver must be current verifier");
        var requiredComment = requireText(comment, COMMENT_FIELD);

        this.resolver = resolver;
        changeStatusTo(IssueStatus.RESOLVED, requiredComment, resolver, changedDate);
    }

    public void close(User changedBy, String comment, LocalDateTime changedDate) {
        requireStatus(IssueStatus.RESOLVED);
        requireRole(changedBy, Role.PL, "changedBy");
        var requiredComment = requireText(comment, COMMENT_FIELD);

        changeStatusTo(IssueStatus.CLOSED, requiredComment, changedBy, changedDate);
        assignee = null;
        verifier = null;
    }

    public void reopen(User changedBy, String comment, LocalDateTime changedDate) {
        if (status != IssueStatus.RESOLVED && status != IssueStatus.CLOSED) {
            throw new IllegalStateException("Issue status must be RESOLVED or CLOSED");
        }
        requireRole(changedBy, Role.PL, "changedBy");
        var requiredComment = requireText(comment, COMMENT_FIELD);

        assignee = null;
        verifier = null;
        changeStatusTo(IssueStatus.REOPENED, requiredComment, changedBy, changedDate);
    }

    public IssueDependency addDependency(
            String dependencyId,
            Issue blockingIssue,
            User changedBy,
            LocalDateTime discoveredDate
    ) {
        Objects.requireNonNull(changedBy, CHANGED_BY_REQUIRED);
        Objects.requireNonNull(blockingIssue, "blockingIssue must not be null");
        rejectDuplicateDependency(blockingIssue);
        var dependency = IssueDependency.create(dependencyId, blockingIssue, this, discoveredDate);
        blockedByDependencies.add(dependency);
        blockingIssue.blockingDependencies.add(dependency);
        recordHistory(
                ActionType.DEPENDENCY_CHANGED,
                null,
                dependencyId,
                "Dependency added",
                changedBy,
                discoveredDate
        );
        return dependency;
    }

    public Comment addComment(String commentId, String content, User writer, LocalDateTime createdDate) {
        var comment = Comment.create(commentId, content, writer, createdDate);
        comments.add(comment);
        recordHistory(ActionType.COMMENTED, null, commentId, content, writer, createdDate);
        return comment;
    }

    public void changePriority(Priority newPriority, User changedBy, LocalDateTime changedDate) {
        Objects.requireNonNull(newPriority, "newPriority must not be null");
        Objects.requireNonNull(changedBy, CHANGED_BY_REQUIRED);
        Objects.requireNonNull(changedDate, CHANGED_DATE_REQUIRED);
        if (priority == newPriority) {
            throw new IllegalArgumentException("newPriority must be different from current priority");
        }

        var previousPriority = priority;
        priority = newPriority;
        recordHistory(
                ActionType.PRIORITY_CHANGED,
                previousPriority.name(),
                newPriority.name(),
                "Priority changed",
                changedBy,
                changedDate
        );
    }

    private void changeStatusTo(IssueStatus targetStatus, String message, User changedBy, LocalDateTime changedDate) {
        Objects.requireNonNull(targetStatus, "targetStatus must not be null");
        Objects.requireNonNull(changedBy, CHANGED_BY_REQUIRED);
        Objects.requireNonNull(changedDate, CHANGED_DATE_REQUIRED);
        if (status == targetStatus) {
            throw new IllegalArgumentException("targetStatus must be different from current status");
        }

        var previousStatus = status;
        status = targetStatus;
        recordHistory(
                ActionType.STATUS_CHANGED,
                previousStatus.name(),
                targetStatus.name(),
                message,
                changedBy,
                changedDate
        );
    }

    private void recordHistory(
            ActionType action,
            String previousValue,
            String newValue,
            String message,
            User changedBy,
            LocalDateTime changedDate
    ) {
        histories.add(IssueHistory.create(
                nextHistoryId(),
                action,
                previousValue,
                newValue,
                message,
                changedBy,
                changedDate
        ));
    }

    private String nextHistoryId() {
        return issueId + "-H" + (histories.size() + 1);
    }

    private void assign(User assignee, User verifier, User changedBy, LocalDateTime changedDate, String message) {
        requireRole(assignee, Role.DEV, "assignee");
        requireRole(verifier, Role.TESTER, "verifier");
        requireRole(changedBy, Role.PL, "changedBy");
        Objects.requireNonNull(changedDate, CHANGED_DATE_REQUIRED);

        this.assignee = assignee;
        this.verifier = verifier;
        recordHistory(
                ActionType.ASSIGNMENT_CHANGED,
                null,
                assignee.getUserId() + "/" + verifier.getUserId(),
                message,
                changedBy,
                changedDate
        );

        var previousStatus = status;
        status = IssueStatus.ASSIGNED;
        recordHistory(
                ActionType.STATUS_CHANGED,
                previousStatus.name(),
                IssueStatus.ASSIGNED.name(),
                message,
                changedBy,
                changedDate
        );
    }

    private void requireStatus(IssueStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException("Issue status must be " + expectedStatus);
        }
    }

    private static void requireRole(User user, Role expectedRole, String fieldName) {
        Objects.requireNonNull(user, fieldName + " must not be null");
        if (!user.isActive()) {
            throw new IllegalArgumentException(fieldName + " must be active");
        }
        if (!user.hasRole(expectedRole)) {
            throw new IllegalArgumentException(fieldName + " must have role " + expectedRole);
        }
    }

    private static void requireCurrentParticipant(User actual, User expected, String message) {
        if (!sameUser(actual, expected)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static boolean sameUser(User first, User second) {
        return first != null && second != null && Objects.equals(first.getUserId(), second.getUserId());
    }

    private void rejectDuplicateDependency(Issue blockingIssue) {
        var blockingIssueId = blockingIssue.getIssueId();
        for (var dependency : blockedByDependencies) {
            if (Objects.equals(dependency.getBlockingIssue().getIssueId(), blockingIssueId)) {
                throw new IllegalArgumentException("Dependency already exists");
            }
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
