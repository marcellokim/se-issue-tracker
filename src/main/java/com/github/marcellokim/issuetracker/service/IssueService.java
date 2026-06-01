package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.repository.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.repository.IssueDependencyRepository;
import com.github.marcellokim.issuetracker.repository.IssueHistoryRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class IssueService {

    private static final String FIELD_PROJECT_ID = "projectId";
    private static final String FIELD_ISSUE_ID = "issueId";
    private static final String FIELD_COMMENT_ID = "commentId";
    private static final String FIELD_CURRENT_LOGIN_ID = "currentLoginId";
    private static final String FIELD_BLOCKED_ISSUE_ID = "blockedIssueId";
    private static final String FIELD_BLOCKING_ISSUE_ID = "blockingIssueId";

    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final IssueDependencyRepository dependencyRepository;
    private final CommentRepository commentRepository;
    private final IssueHistoryRepository issueHistoryRepository;
    private final UserRepository userRepository;
    private final PermissionPolicy permissionPolicy;
    private final IssueIdProvider issueIdProvider;
    private final Clock clock;

    public IssueService(
            ProjectRepository projectRepository,
            IssueRepository issueRepository,
            IssueDependencyRepository dependencyRepository,
            CommentRepository commentRepository,
            IssueHistoryRepository issueHistoryRepository,
            UserRepository userRepository,
            PermissionPolicy permissionPolicy,
            IssueIdProvider issueIdProvider,
            Clock clock) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.dependencyRepository = Objects.requireNonNull(dependencyRepository, "dependencyRepository");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository");
        this.issueHistoryRepository = Objects.requireNonNull(issueHistoryRepository, "issueHistoryRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.issueIdProvider = Objects.requireNonNull(issueIdProvider, "issueIdProvider");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public IssueResult registerIssue(long projectId, String title, String description, Priority priority,
            String currentLoginId) {
        long requiredProjectId = requirePositive(projectId, FIELD_PROJECT_ID);
        String requiredTitle = requireText(title, "title");
        String requiredDescription = requireText(description, "description");
        String requiredLoginId = requireText(currentLoginId, FIELD_CURRENT_LOGIN_ID);
        Project project = findProject(requiredProjectId);
        User reporter = findUser(requiredLoginId);
        permissionPolicy.assertCanRegisterIssue(reporter, project);
        requireActiveProjectMember(reporter, project.getId(), "Only project members can register issues.");
        if (issueRepository.existsByProjectIdAndTitle(project.getId(), requiredTitle)) {
            throw new IllegalArgumentException("Issue title already exists in this project.");
        }
        LocalDateTime now = now();
        Issue issue = Issue.create(
                Issue.persistedState(project.getId(), requiredTitle, requiredDescription, reporter)
                        .issueId(issueIdProvider.nextIssueId())
                        .priority(priority != null ? priority : Priority.MAJOR)
                        .reportedDate(now)
                        .updatedAt(now));
        Issue saved = issueRepository.save(issue);
        return toIssueResult(saved);
    }

    public boolean canRegisterIssue(long projectId, String currentLoginId) {
        try {
            long requiredProjectId = requirePositive(projectId, FIELD_PROJECT_ID);
            String requiredLoginId = requireText(currentLoginId, FIELD_CURRENT_LOGIN_ID);
            Project project = findProject(requiredProjectId);
            User reporter = findUser(requiredLoginId);
            permissionPolicy.assertCanRegisterIssue(reporter, project);
            requireActiveProjectMember(reporter, project.getId(), "Only project members can register issues.");
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public IssueDetailResult viewIssueDetail(long issueId, String currentLoginId) {
        long requiredIssueId = requirePositive(issueId, FIELD_ISSUE_ID);
        String requiredLoginId = requireText(currentLoginId, FIELD_CURRENT_LOGIN_ID);
        Issue issue = findIssueDetail(requiredIssueId, requiredLoginId);
        List<CommentResult> comments = commentRepository.findByIssueId(issue.id()).stream()
                .map(IssueService::toCommentResult)
                .toList();
        List<HistoryResult> histories = issueHistoryRepository.findByIssueId(issue.id()).stream()
                .map(IssueService::toHistoryResult)
                .toList();
        List<IssueDependency> blockedByDeps = dependencyRepository.findDependenciesBlockingIssue(issue.id());
        List<IssueDependency> blockingDeps = dependencyRepository.findDependenciesBlockedByIssue(issue.id());
        Set<Long> relatedIssueIds = new HashSet<>();
        blockedByDeps.forEach(dep -> relatedIssueIds.add(dep.blockingIssueId()));
        blockingDeps.forEach(dep -> relatedIssueIds.add(dep.blockedIssueId()));
        Map<Long, Issue> relatedIssues = issueRepository.findAllById(List.copyOf(relatedIssueIds)).stream()
                .collect(Collectors.toMap(Issue::id, Function.identity()));
        List<DependencyResult> blockedByResults = blockedByDeps.stream()
                .map(dep -> toDependencyResult(dep, relatedIssues.get(dep.blockingIssueId()), issue))
                .toList();
        List<DependencyResult> blockingResults = blockingDeps.stream()
                .map(dep -> toDependencyResult(dep, issue, relatedIssues.get(dep.blockedIssueId())))
                .toList();
        return toIssueDetailResult(issue, comments, histories, blockedByResults, blockingResults);
    }

    public List<IssueSummary> searchIssues(
            long projectId,
            String keyword,
            IssueStatus status,
            Priority priority,
            String reporterId,
            String assigneeId,
            String verifierId,
            LocalDateTime reportedFrom,
            LocalDateTime reportedTo,
            String currentLoginId) {

        SearchContext context = searchContext(
                projectId,
                status,
                reportedFrom,
                reportedTo,
                currentLoginId,
                "Only project members can search issues.");
        return searchIssuesByCriteria(IssueSearchCriteria.create(
                context.projectId(),
                status,
                priority,
                optionalText(reporterId),
                optionalText(assigneeId),
                optionalText(verifierId),
                optionalText(keyword),
                reportedFrom,
                reportedTo,
                false)).stream()
                .map(IssueService::toIssueSummary)
                .toList();
    }

    public List<IssueSummary> viewProjectIssues(long projectId, String currentLoginId) {
        long requiredProjectId = requirePositive(projectId, FIELD_PROJECT_ID);
        String requiredLoginId = requireText(currentLoginId, FIELD_CURRENT_LOGIN_ID);
        findProject(requiredProjectId);
        User actor = findUser(requiredLoginId);
        permissionPolicy.assertCanViewIssue(actor);
        requireActiveProjectMember(actor, requiredProjectId, "Only project members can view project issues.");
        return issueRepository.findByCriteria(IssueSearchCriteria.create(
                requiredProjectId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false)).stream()
                .map(IssueService::toIssueSummary)
                .toList();
    }

    public IssueResult updateIssue(long issueId, String title, String description, String currentLoginId) {
        long requiredIssueId = requirePositive(issueId, FIELD_ISSUE_ID);
        String requiredLoginId = requireText(currentLoginId, FIELD_CURRENT_LOGIN_ID);
        String requiredTitle = requireText(title, "title");
        String requiredDescription = requireText(description, "description");

        Issue issue = findIssue(requiredIssueId);
        requireNotDeleted(issue);
        User actor = findUser(requiredLoginId);
        permissionPolicy.assertCanUpdateIssue(actor, issue);
        requireActiveProjectMember(actor, issue.projectId(), "Only project members can update issues.");
        if (issueRepository.existsByProjectIdAndTitleExcludingIssueId(issue.projectId(), requiredTitle, issue.id())) {
            throw new IllegalArgumentException("Issue title already exists in this project.");
        }
        issue.updateTitleAndDescription(requiredTitle, requiredDescription, actor, now());
        Issue saved = issueRepository.save(issue);
        return toIssueResult(saved);
    }

    public IssueResult changePriority(long issueId, Priority priority, String currentLoginId) {
        long requiredIssueId = requirePositive(issueId, FIELD_ISSUE_ID);
        String requiredLoginId = requireText(currentLoginId, FIELD_CURRENT_LOGIN_ID);
        Issue issue = findIssue(requiredIssueId);
        requireNotDeleted(issue);
        User actor = findUser(requiredLoginId);
        permissionPolicy.assertCanChangePriority(actor, issue, priority);
        requireProjectLead(actor, issue.projectId(), "Only the project PL can change issue priority.");
        issue.changePriority(priority, actor, now());
        Issue saved = issueRepository.save(issue);
        return toIssueResult(saved);
    }

    public CommentResult addComment(long issueId, String content, String currentLoginId) {
        long requiredIssueId = requirePositive(issueId, FIELD_ISSUE_ID);
        String requiredContent = requireText(content, "content");
        String requiredLoginId = requireText(currentLoginId, FIELD_CURRENT_LOGIN_ID);
        Issue issue = findIssue(requiredIssueId);
        requireNotDeleted(issue);
        User writer = findUser(requiredLoginId);
        permissionPolicy.assertCanAddComment(writer, issue);
        requireActiveProjectMember(writer, issue.projectId(), "Only project members can add issue comments.");
        LocalDateTime now = now();
        Comment comment = Comment.newForIssue(issue.id(), requiredContent, writer, CommentPurpose.GENERAL, now);
        IssueHistory history = IssueHistory.newForPersistence(
                issue.id(),
                writer.getLoginId(),
                ActionType.COMMENTED,
                null,
                requiredContent,
                "comment added",
                now);
        Comment saved = commentRepository.saveCommentAndRecordHistory(comment, history);
        return toCommentResult(saved, writer);
    }

    public List<CommentResult> viewComments(long issueId, String currentLoginId) {
        long requiredIssueId = requirePositive(issueId, FIELD_ISSUE_ID);
        String requiredLoginId = requireText(currentLoginId, FIELD_CURRENT_LOGIN_ID);
        Issue issue = findIssue(requiredIssueId);
        requireNotDeleted(issue);
        User actor = findUser(requiredLoginId);
        permissionPolicy.assertCanViewIssue(actor);
        requireActiveProjectMember(actor, issue.projectId(), "Only project members can view comments.");
        return commentRepository.findByIssueId(issue.id()).stream()
                .map(IssueService::toCommentResult)
                .toList();
    }

    public DependencyResult addDependency(long blockingIssueId, long blockedIssueId, String currentLoginId) {
        String requiredLoginId = requireText(currentLoginId, FIELD_CURRENT_LOGIN_ID);
        long requiredBlockedIssueId = requirePositive(blockedIssueId, FIELD_BLOCKED_ISSUE_ID);
        long requiredBlockingIssueId = requirePositive(blockingIssueId, FIELD_BLOCKING_ISSUE_ID);
        Issue blockingIssue = findIssue(requiredBlockingIssueId);
        Issue blockedIssue = findIssue(requiredBlockedIssueId);
        requireNotDeleted(blockingIssue);
        requireNotDeleted(blockedIssue);
        requireSameProjectDependency(blockingIssue, blockedIssue);
        User actor = findUser(requiredLoginId);
        permissionPolicy.assertCanManageDependency(actor, blockedIssue);
        requireProjectLead(actor, blockedIssue.projectId(), "Only the project PL can manage dependencies.");
        validateDependency(requiredBlockingIssueId, requiredBlockedIssueId);
        LocalDateTime now = now();
        String dependencyId = IssueDependency.dependencyIdFor(requiredBlockingIssueId, requiredBlockedIssueId);
        IssueDependency dependency = blockedIssue.addDependency(dependencyId, blockingIssue, actor, now);
        IssueDependency saved = dependencyRepository.recordDependencyAdded(dependency, blockedIssue);
        return toDependencyResult(saved, blockingIssue, blockedIssue);
    }

    public List<DependencyResult> viewProjectDependencies(long projectId, String currentLoginId) {
        long requiredProjectId = requirePositive(projectId, FIELD_PROJECT_ID);
        String requiredLoginId = requireText(currentLoginId, FIELD_CURRENT_LOGIN_ID);
        findProject(requiredProjectId);
        User actor = findUser(requiredLoginId);
        permissionPolicy.assertCanViewIssue(actor);
        requireActiveProjectMember(actor, requiredProjectId, "Only project members can view project dependencies.");
        List<IssueDependency> deps = dependencyRepository.findByProjectId(requiredProjectId);
        Set<Long> issueIds = new HashSet<>();
        for (IssueDependency dep : deps) {
            issueIds.add(dep.blockingIssueId());
            issueIds.add(dep.blockedIssueId());
        }
        Map<Long, Issue> issuesById = issueRepository.findAllById(List.copyOf(issueIds)).stream()
                .collect(Collectors.toMap(Issue::id, Function.identity()));
        return deps.stream()
                .map(dep -> toDependencyResult(dep, issuesById.get(dep.blockingIssueId()),
                        issuesById.get(dep.blockedIssueId())))
                .toList();
    }

    public void removeDependency(long blockingIssueId, long blockedIssueId, String currentLoginId) {
        String requiredLoginId = requireText(currentLoginId, FIELD_CURRENT_LOGIN_ID);
        long requiredBlockedIssueId = requirePositive(blockedIssueId, FIELD_BLOCKED_ISSUE_ID);
        long requiredBlockingIssueId = requirePositive(blockingIssueId, FIELD_BLOCKING_ISSUE_ID);
        String hashedDependencyId = IssueDependency.dependencyIdFor(requiredBlockingIssueId, requiredBlockedIssueId);
        IssueDependency dependency = dependencyRepository.findByDependencyId(hashedDependencyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Dependency not found: " + requiredBlockingIssueId + " -> " + requiredBlockedIssueId));
        Issue blockingIssue = findIssue(dependency.blockingIssueId());
        Issue blockedIssue = findIssue(dependency.blockedIssueId());
        requireNotDeleted(blockingIssue);
        requireNotDeleted(blockedIssue);
        User actor = findUser(requiredLoginId);
        permissionPolicy.assertCanManageDependency(actor, blockedIssue);
        requireProjectLead(actor, blockedIssue.projectId(), "Only the project PL can manage dependencies.");
        requireSameProjectDependency(blockingIssue, blockedIssue);
        blockedIssue.removeDependency(dependency, actor, now());
        dependencyRepository.recordDependencyRemoved(hashedDependencyId, blockedIssue);
    }

    public void deleteComment(long issueId, long commentId, String currentLoginId) {
        long requiredIssueId = requirePositive(issueId, FIELD_ISSUE_ID);
        long requiredCommentId = requirePositive(commentId, FIELD_COMMENT_ID);
        String requiredLoginId = requireText(currentLoginId, FIELD_CURRENT_LOGIN_ID);
        Issue issue = findIssue(requiredIssueId);
        requireNotDeleted(issue);
        Comment comment = findComment(requiredCommentId);
        User currentUser = findUser(requiredLoginId);
        requireCommentBelongsToIssue(comment, issue);
        permissionPolicy.assertCanDeleteComment(currentUser, comment);
        requireActiveProjectMember(currentUser, issue.projectId(), "Only project members can delete issue comments.");

        issue.recordCommentDeletion(comment, currentUser, now());
        IssueHistory history = issue.getHistories().getLast();
        commentRepository.deleteGeneralByIdAndRecordIssueChange(
                issue.id(),
                comment.id(),
                currentUser.getLoginId(),
                historyForPersistence(issue.id(), history));
    }

    public CommentResult updateComment(long issueId, long commentId, String content, String currentLoginId) {
        long requiredIssueId = requirePositive(issueId, FIELD_ISSUE_ID);
        long requiredCommentId = requirePositive(commentId, FIELD_COMMENT_ID);
        String requiredContent = requireText(content, "content");
        String requiredLoginId = requireText(currentLoginId, FIELD_CURRENT_LOGIN_ID);
        Issue issue = findIssue(requiredIssueId);
        requireNotDeleted(issue);
        Comment comment = findComment(requiredCommentId);
        User currentUser = findUser(requiredLoginId);
        requireCommentBelongsToIssue(comment, issue);
        permissionPolicy.assertCanUpdateComment(currentUser, comment);
        requireActiveProjectMember(currentUser, issue.projectId(), "Only project members can update issue comments.");

        String previousContent = comment.content();
        if (previousContent.equals(requiredContent)) {
            throw new IllegalArgumentException("comment content is same as current content.");
        }
        LocalDateTime changedAt = now();
        comment.changeContent(requiredContent, changedAt);
        IssueHistory history = IssueHistory.newForPersistence(
                issue.id(),
                currentUser.getLoginId(),
                ActionType.COMMENTED,
                previousContent,
                comment.content(),
                "comment updated",
                changedAt);
        Comment saved = commentRepository.saveCommentAndRecordHistory(comment, history);
        return toCommentResult(saved);
    }

    private Project findProject(long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
    }

    private Issue findIssue(long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
    }

    private Issue findIssueDetail(long issueId, String currentLoginId) {
        User actor = findUser(currentLoginId);
        permissionPolicy.assertCanViewIssue(actor);
        Issue issue = findIssue(issueId);
        requireNotDeleted(issue);
        requireActiveProjectMember(actor, issue.projectId(), "Only project members can view issue details.");
        return issue;
    }

    private Comment findComment(long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
    }

    private User findUser(String userId) {
        return userRepository.findByLoginId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private void requireProjectLead(User actor, long projectId, String message) {
        boolean projectLead = actor.getRole() == Role.PL
                && userRepository.existsActiveProjectMember(projectId, actor.getLoginId());
        if (!projectLead) {
            throw new SecurityException(message);
        }
    }

    private void requireActiveProjectMember(User actor, long projectId, String message) {
        if (!isActiveProjectMember(actor, projectId)) {
            throw new SecurityException(message);
        }
    }

    private boolean isActiveProjectMember(User actor, long projectId) {
        return userRepository.existsActiveProjectMember(projectId, actor.getLoginId());
    }

    private SearchContext searchContext(
            long projectId,
            IssueStatus status,
            LocalDateTime reportedFrom,
            LocalDateTime reportedTo,
            String currentLoginId,
            String membershipMessage) {
        long requiredProjectId = requirePositive(projectId, FIELD_PROJECT_ID);
        String requiredLoginId = requireText(currentLoginId, FIELD_CURRENT_LOGIN_ID);
        if (reportedFrom != null && reportedTo != null && reportedFrom.isAfter(reportedTo)) {
            throw new IllegalArgumentException("reportedFrom must not be after reportedTo.");
        }
        findProject(requiredProjectId);
        User actor = findUser(requiredLoginId);
        permissionPolicy.assertCanViewIssue(actor);
        if (status == IssueStatus.DELETED) {
            throw new SecurityException("Deleted issues must be managed through deleted issue workflow.");
        }
        requireActiveProjectMember(actor, requiredProjectId, membershipMessage);
        return new SearchContext(requiredProjectId, actor);
    }

    private List<Issue> searchIssuesByCriteria(IssueSearchCriteria criteria) {
        return issueRepository.findByCriteria(criteria).stream()
                .filter(issue -> issue.projectId() == criteria.projectId())
                .toList();
    }

    private static void requireSameProjectDependency(Issue blockingIssue, Issue blockedIssue) {
        if (blockingIssue.projectId() != blockedIssue.projectId()) {
            throw new IllegalArgumentException("Dependencies are allowed only within the same project.");
        }
    }

    private static void requireNotDeleted(Issue issue) {
        if (issue.status() == IssueStatus.DELETED) {
            throw new SecurityException("Deleted issues must be managed through deleted issue workflow.");
        }
    }

    private void validateDependency(long blockingIssueId, long blockedIssueId) {
        if (blockingIssueId == blockedIssueId) {
            throw new IllegalArgumentException("Issue cannot depend on itself");
        }
        if (dependencyRepository.existsByPair(blockingIssueId, blockedIssueId)) {
            throw new IllegalArgumentException("Dependency already exists");
        }
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(blockingIssueId);
        while (!queue.isEmpty()) {
            long current = queue.poll();
            if (current == blockedIssueId) {
                throw new IllegalArgumentException("Circular dependency detected");
            }
            if (!visited.add(current)) {
                continue;
            }
            for (var dep : dependencyRepository.findDependenciesBlockingIssue(current)) {
                queue.add(dep.blockingIssueId());
            }
        }
    }

    private static void requireCommentBelongsToIssue(Comment comment, Issue issue) {
        if (comment.issueId() != issue.id()) {
            throw new IllegalArgumentException("Comment does not belong to the issue.");
        }
    }

    private static IssueHistory historyForPersistence(long issueId, IssueHistory history) {
        return IssueHistory.newForPersistence(
                issueId,
                history.changedById(),
                history.actionType(),
                history.previousValue(),
                history.newValue(),
                history.message(),
                history.changedDate());
    }

    private static String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private LocalDateTime now() {
        return clock.now();
    }

    private record SearchContext(long projectId, User actor) {
    }

    private static IssueResult toIssueResult(Issue issue) {
        return new IssueResult(
                issue.id(),
                issue.getIssueId(),
                issue.status(),
                issue.priority(),
                issue.title(),
                issue.description(),
                toUserResult(issue.getReporter()));
    }

    private static IssueSummary toIssueSummary(Issue issue) {
        return new IssueSummary(
                issue.id(),
                issue.getIssueId(),
                issue.projectId(),
                issue.status(),
                issue.priority(),
                issue.title(),
                issue.reporterId(),
                issue.assigneeId(),
                issue.verifierId(),
                issue.reportedDate(),
                issue.updatedAt());
    }

    private static IssueDetailResult toIssueDetailResult(
            Issue issue,
            List<CommentResult> comments,
            List<HistoryResult> histories,
            List<DependencyResult> blockedByDependencies,
            List<DependencyResult> blockingDependencies) {
        return new IssueDetailResult(
                issue.id(),
                issue.projectId(),
                issue.getIssueId(),
                issue.status(),
                issue.priority(),
                issue.title(),
                issue.description(),
                toUserResult(issue.getReporter()),
                toUserResult(issue.getAssignee()),
                toUserResult(issue.getVerifier()),
                toUserResult(issue.getFixer()),
                toUserResult(issue.getResolver()),
                issue.reportedDate(),
                issue.updatedAt(),
                comments,
                histories,
                blockedByDependencies,
                blockingDependencies,
                List.of());
    }

    private static CommentResult toCommentResult(Comment comment) {
        return toCommentResult(comment, comment.getWriter());
    }

    private static CommentResult toCommentResult(Comment comment, User writer) {
        return new CommentResult(
                comment.getCommentId(),
                comment.getContent(),
                comment.getPurpose(),
                comment.writerId(),
                toUserResult(writer),
                comment.getCreatedDate(),
                comment.getUpdatedDate());
    }

    private static UserResult toUserResult(User user) {
        return user == null ? null : UserResult.from(user);
    }

    private static HistoryResult toHistoryResult(IssueHistory history) {
        return new HistoryResult(
                history.id(),
                history.issueId(),
                history.changedById(),
                history.actionType(),
                history.previousValue(),
                history.newValue(),
                history.message(),
                history.changedDate());
    }

    private static DependencyResult toDependencyResult(IssueDependency dep, Issue blockingIssue, Issue blockedIssue) {
        return new DependencyResult(
                dep.id(),
                dep.getDependencyId(),
                blockingIssue.id(),
                blockingIssue.getIssueId(),
                blockedIssue.id(),
                blockedIssue.getIssueId(),
                dep.getDiscoveredDate());
    }
}
