package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Issue {

    private static final String CREATED_PREVIOUS_VALUE = null;
    private static final String COMMENT_FIELD = "comment";
    private static final String CHANGED_BY_REQUIRED = "changedBy must not be null";
    private static final String CHANGED_DATE_REQUIRED = "changedDate must not be null";
    private static final String ISSUE_ID_PREFIX = "ISSUE-";

    private final long id;
    private final long projectId;
    private final String issueId;
    private String title;
    private String description;
    private final LocalDateTime reportedDate;
    private Priority priority;
    private IssueStatus status;
    private final User reporter;
    private User assignee;
    private User verifier;
    private User fixer;
    private User resolver;
    private final String reporterId;
    private String assigneeId;
    private String verifierId;
    private String fixerId;
    private String resolverId;
    private LocalDateTime updatedAt;
    private final List<Comment> comments = new ArrayList<>();
    private final List<IssueHistory> histories = new ArrayList<>();
    private final List<IssueDependency> blockingDependencies = new ArrayList<>();
    private final List<IssueDependency> blockedByDependencies = new ArrayList<>();

    private Issue(PersistedState state) {
        this(state, true);
    }

    private Issue(PersistedState state, boolean persisted) {
        Objects.requireNonNull(state, "state must not be null");
        this.id = persisted ? requirePositive(state.id, "id") : requireZero(state.id, "id");
        this.projectId = requirePositive(state.projectId, "projectId");
        this.issueId = persisted ? requireText(state.issueId, "issueId") : issueIdOrNew(state.issueId);
        this.title = requireText(state.title, "title");
        this.description = requireText(state.description, "description");
        this.reportedDate = Objects.requireNonNull(state.reportedDate, "reportedDate must not be null");
        this.priority = Objects.requireNonNull(state.priority, "priority must not be null");
        this.status = Objects.requireNonNull(state.status, "status must not be null");
        this.reporter = Objects.requireNonNull(state.reporter, "reporter must not be null");
        this.assignee = state.assignee;
        this.verifier = state.verifier;
        this.fixer = state.fixer;
        this.resolver = state.resolver;
        this.reporterId = reporter.getLoginId();
        this.assigneeId = loginIdOrNull(assignee);
        this.verifierId = loginIdOrNull(verifier);
        this.fixerId = loginIdOrNull(fixer);
        this.resolverId = loginIdOrNull(resolver);
        this.updatedAt = Objects.requireNonNull(state.updatedAt, "updatedAt must not be null");
    }

    private Issue(
            String issueId,
            String title,
            String description,
            Priority priority,
            User reporter,
            LocalDateTime reportedDate
    ) {
        this.id = 0L;
        this.projectId = 0L;
        this.issueId = requireText(issueId, "issueId");
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.priority = priority == null ? Priority.MAJOR : priority;
        this.status = IssueStatus.NEW;
        this.reporter = Objects.requireNonNull(reporter, "reporter must not be null");
        this.reporterId = reporter.getLoginId();
        this.reportedDate = Objects.requireNonNull(reportedDate, "reportedDate must not be null");
        this.updatedAt = reportedDate;
        recordHistory(ActionType.CREATED, CREATED_PREVIOUS_VALUE, IssueStatus.NEW.name(), "Issue created", reporter, reportedDate);
    }

    public static Issue create( // 테스트용 생성 메서드, 실제로는 Persistence 계층에서 PersistedState를 통해 생성
            String issueId,
            String title,
            String description,
            Priority priority,
            User reporter,
            LocalDateTime reportedDate
    ) {
        return new Issue(issueId, title, description, priority, reporter, reportedDate);
    }

    public static PersistedState persistedState(long projectId, String title, String description, User reporter) {
        return new PersistedState(projectId, title, description, reporter);
    }

    public static Issue fromPersistence(PersistedState state) {
        return new Issue(state);
    }

    public static Issue newForPersistence(PersistedState state) {
        return new Issue(state, false);
    }

    // --- getters ---
    public long id() {
        return id;
    }

    public long projectId() {
        return projectId;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public LocalDateTime reportedDate() {
        return reportedDate;
    }

    public Priority priority() {
        return priority;
    }

    public IssueStatus status() {
        return status;
    }

    public String reporterId() {
        return reporterId;
    }

    public String assigneeId() {
        return assigneeId;
    }

    public String verifierId() {
        return verifierId;
    }

    public String fixerId() {
        return fixerId;
    }

    public String resolverId() {
        return resolverId;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
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

    // --- status management ---
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
        this.assigneeId = assignee.getLoginId();
        updatedAt = changedDate;
        recordHistory(
                ActionType.ASSIGNMENT_CHANGED,
                previousAssignee == null ? null : previousAssignee.getLoginId(),
                assignee.getLoginId(),
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
        this.verifierId = verifier.getLoginId();
        updatedAt = changedDate;
        recordHistory(
                ActionType.ASSIGNMENT_CHANGED,
                previousVerifier == null ? null : previousVerifier.getLoginId(),
                verifier.getLoginId(),
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
        this.fixerId = fixer.getLoginId();
        changeStatusTo(IssueStatus.FIXED, requiredComment, fixer, changedDate);
    }

    public void resolve(User resolver, String comment, LocalDateTime changedDate) {
        requireStatus(IssueStatus.FIXED);
        requireRole(resolver, Role.TESTER, "resolver");
        requireCurrentParticipant(resolver, verifier, "resolver must be current verifier");
        var requiredComment = requireText(comment, COMMENT_FIELD);

        this.resolver = resolver;
        this.resolverId = resolver.getLoginId();
        changeStatusTo(IssueStatus.RESOLVED, requiredComment, resolver, changedDate);
    }

    public void close(User changedBy, String comment, LocalDateTime changedDate) {
        requireStatus(IssueStatus.RESOLVED);
        requireRole(changedBy, Role.PL, "changedBy");
        var requiredComment = requireText(comment, COMMENT_FIELD);

        changeStatusTo(IssueStatus.CLOSED, requiredComment, changedBy, changedDate);
        assignee = null;
        verifier = null;
        assigneeId = null;
        verifierId = null;
    }

    public void reopen(User changedBy, String comment, LocalDateTime changedDate) {
        if (status != IssueStatus.RESOLVED && status != IssueStatus.CLOSED) {
            throw new IllegalStateException("Issue status must be RESOLVED or CLOSED");
        }
        requireRole(changedBy, Role.PL, "changedBy");
        var requiredComment = requireText(comment, COMMENT_FIELD);

        assignee = null;
        verifier = null;
        assigneeId = null;
        verifierId = null;
        changeStatusTo(IssueStatus.REOPENED, requiredComment, changedBy, changedDate);
    }

    public void rejectFix(User tester, String comment, LocalDateTime changedDate) {
        requireStatus(IssueStatus.FIXED);
        requireRole(tester, Role.TESTER, "tester");
        requireCurrentParticipant(tester, verifier, "tester must be current verifier");
        var requiredComment = requireText(comment, COMMENT_FIELD);
        changeStatusTo(IssueStatus.ASSIGNED, requiredComment, tester, changedDate);
    }

    // --- other methods ---
    public IssueDependency addDependency(
            String dependencyId,
            Issue blockingIssue,
            User changedBy,
            LocalDateTime discoveredDate
    ) {
        Objects.requireNonNull(changedBy, CHANGED_BY_REQUIRED);
        Objects.requireNonNull(blockingIssue, "blockingIssue must not be null");
        rejectSelfDependency(blockingIssue);
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
        return addComment(commentId, content, writer, createdDate, CommentPurpose.GENERAL);
    }

    public Comment addComment(
            String commentId,
            String content,
            User writer,
            LocalDateTime createdDate,
            CommentPurpose purpose
    ) {
        var comment = Comment.create(commentId, content, writer, purpose, createdDate);
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
        updatedAt = changedDate;
        recordHistory(
                ActionType.PRIORITY_CHANGED,
                previousPriority.name(),
                newPriority.name(),
                "Priority changed",
                changedBy,
                changedDate
        );
    }
    public void updateTitleAndDescription(
        String newTitle,
        String newDescription,
        User changer,
        LocalDateTime changedDate
    ) {
        Objects.requireNonNull(changer, CHANGED_BY_REQUIRED);
        Objects.requireNonNull(changedDate, CHANGED_DATE_REQUIRED);

        if (!sameUser(changer, reporter)) {
            throw new IllegalArgumentException("changer must be Reporter");
        }
        if (status != IssueStatus.NEW && status != IssueStatus.REOPENED) {
            throw new IllegalStateException("Issue status must be before Assigned");
        }
        String previousValue = title + "\n" + description;
        this.title = requireText(newTitle, "title");
        this.description = requireText(newDescription, "description");
        this.updatedAt = changedDate;
        recordHistory(
            ActionType.TITLE_DESCRIPTION_UPDATED,
            previousValue,
            title + "\n" + description,
            "Title and description changed",
            changer,
            changedDate
        );
    }

    // --- general-purpose private methods ---
    private void changeStatusTo(IssueStatus targetStatus, String message, User changedBy, LocalDateTime changedDate) {
        Objects.requireNonNull(targetStatus, "targetStatus must not be null");
        Objects.requireNonNull(changedBy, CHANGED_BY_REQUIRED);
        Objects.requireNonNull(changedDate, CHANGED_DATE_REQUIRED);
        if (status == targetStatus) {
            throw new IllegalArgumentException("targetStatus must be different from current status");
        }

        var previousStatus = status;
        status = targetStatus;
        updatedAt = changedDate;
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
        this.assigneeId = assignee.getLoginId();
        this.verifierId = verifier.getLoginId();
        updatedAt = changedDate;
        recordHistory(
                ActionType.ASSIGNMENT_CHANGED,
                null,
                assignee.getLoginId() + "/" + verifier.getLoginId(),
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
        return first != null && second != null && Objects.equals(first.getLoginId(), second.getLoginId());
    }

    private void rejectSelfDependency(Issue blockingIssue) {
        if (Objects.equals(blockingIssue.getIssueId(), issueId)) {
            throw new IllegalArgumentException("Issue cannot depend on itself");
        }
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

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static long requireZero(long value, String fieldName) {
        if (value != 0L) {
            throw new IllegalArgumentException(fieldName + " must be zero before persistence");
        }
        return value;
    }

    private static String newIssueId() {
        return ISSUE_ID_PREFIX + UUID.randomUUID().toString();
    }

    private static String issueIdOrNew(String issueId) {
        if (issueId == null || issueId.isBlank()) {
            return newIssueId();
        }
        return requireText(issueId, "issueId");
    }

    private static String loginIdOrNull(User user) {
        return user == null ? null : user.getLoginId();
    }

    public static final class PersistedState {

        private long id;
        private String issueId;
        private final long projectId;
        private final String title;
        private final String description;
        private final User reporter;
        private LocalDateTime reportedDate;
        private Priority priority = Priority.MAJOR;
        private IssueStatus status = IssueStatus.NEW;
        private User assignee;
        private User verifier;
        private User fixer;
        private User resolver;
        private LocalDateTime updatedAt;

        private PersistedState(long projectId, String title, String description, User reporter) {
            this.projectId = projectId;
            this.title = requireText(title, "title");
            this.description = requireText(description, "description");
            this.reporter = Objects.requireNonNull(reporter, "reporter must not be null");
        }

        public PersistedState id(long id) {
            this.id = id;
            return this;
        }

        public PersistedState issueId(String issueId) {
            this.issueId = requireText(issueId, "issueId");
            return this;
        }

        public PersistedState reportedDate(LocalDateTime reportedDate) {
            this.reportedDate = reportedDate;
            return this;
        }

        public PersistedState priority(Priority priority) {
            this.priority = Objects.requireNonNull(priority, "priority must not be null");
            return this;
        }

        public PersistedState status(IssueStatus status) {
            this.status = Objects.requireNonNull(status, "status must not be null");
            return this;
        }

        public PersistedState assignee(User assignee) {
            this.assignee = assignee;
            return this;
        }

        public PersistedState verifier(User verifier) {
            this.verifier = verifier;
            return this;
        }

        public PersistedState fixer(User fixer) {
            this.fixer = fixer;
            return this;
        }

        public PersistedState resolver(User resolver) {
            this.resolver = resolver;
            return this;
        }

        public PersistedState updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
    }
}
