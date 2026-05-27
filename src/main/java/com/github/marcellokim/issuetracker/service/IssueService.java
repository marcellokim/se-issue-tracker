package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
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

    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final IssueDependencyRepository dependencyRepository;
    private final CommentRepository commentRepository;
    private final IssueHistoryRepository issueHistoryRepository;
    private final UserRepository userRepository;
    private final PermissionPolicy permissionPolicy;
    private final CommentIdProvider commentIdProvider;
    private final Clock clock;

    public IssueService(
            ProjectRepository projectRepository,
            IssueRepository issueRepository,
            IssueDependencyRepository dependencyRepository,
            CommentRepository commentRepository,
            IssueHistoryRepository issueHistoryRepository,
            UserRepository userRepository,
            PermissionPolicy permissionPolicy,
            Clock clock,
            CommentIdProvider commentIdProvider) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.dependencyRepository = Objects.requireNonNull(dependencyRepository, "dependencyRepository");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository");
        this.issueHistoryRepository = Objects.requireNonNull(issueHistoryRepository, "issueHistoryRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.commentIdProvider = Objects.requireNonNull(commentIdProvider, "commentIdProvider");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public IssueResult registerIssue(long projectId, String title, String description, Priority priority,
            String currentLoginId) {
        projectId = requirePositive(projectId, "projectId");
        title = requireText(title, "title");
        description = requireText(description, "description");
        currentLoginId = requireText(currentLoginId, "currentLoginId");
        Project project = findProject(projectId);
        User reporter = findUser(currentLoginId);
        permissionPolicy.assertCanRegisterIssue(reporter, project);
        requireActiveProjectMember(reporter, project.getId(), "Only project members can register issues.");
        if (issueRepository.existsByProjectIdAndTitle(project.getId(), title)) {
            throw new IllegalArgumentException("Issue title already exists in this project.");
        }
        LocalDateTime now = now();
        Issue issue = Issue.create(
                Issue.persistedState(project.getId(), title, description, reporter)
                        .priority(priority != null ? priority : Priority.MAJOR)
                        .reportedDate(now)
                        .updatedAt(now));
        Issue saved = issueRepository.save(issue);
        return toIssueResult(saved);
    }

    public IssueDetailResult viewIssueDetail(long issueId, String currentLoginId) {
        issueId = requirePositive(issueId, "issueId");
        currentLoginId = requireText(currentLoginId, "currentLoginId");
        Issue issue = findIssueDetail(issueId, currentLoginId);
        List<CommentResult> comments = commentRepository.findByIssueId(issue.id()).stream()
                .map(IssueService::toCommentResult)
                .toList();
        List<HistoryResult> histories = issueHistoryRepository.findByIssueId(issue.id()).stream()
                .map(IssueService::toHistoryResult)
                .toList();
        List<IssueDependency> deps = dependencyRepository.findByBlockedIssueId(issue.id());
        List<Long> blockingIds = deps.stream()
                .map(IssueDependency::blockingIssueId)
                .toList();
        Map<Long, Issue> blockingIssues = issueRepository.findAllById(blockingIds).stream()
                .collect(Collectors.toMap(Issue::id, Function.identity()));
        List<DependencyResult> dependencies = deps.stream()
                .map(dep -> toDependencyResult(dep, blockingIssues.get(dep.blockingIssueId()), issue))
                .toList();
        return toIssueDetailResult(issue, comments, histories, dependencies);
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

        long requireProjectId = requirePositive(projectId, "projectId");
        currentLoginId = requireText(currentLoginId, "currentLoginId");
        if (reportedFrom != null && reportedTo != null && reportedFrom.isAfter(reportedTo)) {
            throw new IllegalArgumentException("reportedFrom must not be after reportedTo.");
        }
        findProject(requireProjectId);
        User actor = findUser(currentLoginId);
        permissionPolicy.assertCanViewIssue(actor);
        if (status == IssueStatus.DELETED) {
            throw new SecurityException("Deleted issues must be managed through deleted issue workflow.");
        }
        requireActiveProjectMember(actor, requireProjectId, "Only project members can search issues.");
        return issueRepository.findByCriteria(IssueSearchCriteria.create(
                requireProjectId,
                status,
                priority,
                optionalText(reporterId),
                optionalText(assigneeId),
                optionalText(verifierId),
                optionalText(keyword),
                reportedFrom,
                reportedTo,
                false)).stream()
                .filter(issue -> issue.projectId() == requireProjectId)
                .map(IssueService::toIssueSummary)
                .toList();
    }

    public List<IssueSummary> viewRelatedProjectIssues(long projectId, String currentLoginId) {
        projectId = requirePositive(projectId, "projectId");
        currentLoginId = requireText(currentLoginId, "currentLoginId");
        findProject(projectId);
        User actor = findUser(currentLoginId);
        permissionPolicy.assertCanViewIssue(actor);
        requireActiveProjectMember(actor, projectId, "Only project members can view related project issues.");
        return issueRepository.findByCriteria(IssueSearchCriteria.create(
                projectId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false)).stream()
                .filter(issue -> actor.getRole() == Role.PL || isRelatedParticipant(issue, actor.getLoginId()))
                .map(IssueService::toIssueSummary)
                .toList();
    }

    public IssueResult updateIssue(long issueId, String title, String description, String currentLoginId) {
        issueId = requirePositive(issueId, "issueId");
        currentLoginId = requireText(currentLoginId, "currentLoginId");
        title = requireText(title, "title");
        description = requireText(description, "description");

        Issue issue = findIssue(issueId);
        requireNotDeleted(issue);
        User actor = findUser(currentLoginId);
        permissionPolicy.assertCanUpdateIssue(actor, issue);
        requireActiveProjectMember(actor, issue.projectId(), "Only project members can update issues.");
        if (issueRepository.existsByProjectIdAndTitleExcludingIssueId(issue.projectId(), title, issue.id())) {
            throw new IllegalArgumentException("Issue title already exists in this project.");
        }
        issue.updateTitleAndDescription(title, description, actor, now());
        Issue saved = issueRepository.save(issue);
        return toIssueResult(saved);
    }

    public IssueResult changePriority(long issueId, Priority priority, String currentLoginId) {
        issueId = requirePositive(issueId, "issueId");
        currentLoginId = requireText(currentLoginId, "currentLoginId");
        Issue issue = findIssue(issueId);
        requireNotDeleted(issue);
        User actor = findUser(currentLoginId);
        permissionPolicy.assertCanChangePriority(actor, issue, priority);
        requireProjectLead(actor, issue.projectId(), "Only the project PL can change issue priority.");
        issue.changePriority(priority, actor, now());
        Issue saved = issueRepository.save(issue);
        return toIssueResult(saved);
    }

    public CommentResult addComment(long issueId, String content, String currentLoginId) {
        issueId = requirePositive(issueId, "issueId");
        content = requireText(content, "content");
        currentLoginId = requireText(currentLoginId, "currentLoginId");
        Issue issue = findIssue(issueId);
        requireNotDeleted(issue);
        User writer = findUser(currentLoginId);
        permissionPolicy.assertCanAddComment(writer, issue);
        requireActiveProjectMember(writer, issue.projectId(), "Only project members can add issue comments.");
        LocalDateTime now = now();
        Comment comment = issue.addComment(commentIdProvider.nextCommentId(), content, writer, now);
        issueRepository.save(issue);
        return toCommentResult(comment);
    }

    // 중복? 순순히 코멘트만 보여주는 기능
    public List<CommentResult> viewComments(long issueId, String currentLoginId) {
        issueId = requirePositive(issueId, "issueId");
        currentLoginId = requireText(currentLoginId, "currentLoginId");
        Issue issue = findIssue(issueId);
        requireNotDeleted(issue);
        User actor = findUser(currentLoginId);
        permissionPolicy.assertCanViewIssue(actor);
        requireActiveProjectMember(actor, issue.projectId(), "Only project members can view comments.");
        return commentRepository.findByIssueId(issue.id()).stream()
                .map(IssueService::toCommentResult)
                .toList();
    }

    public DependencyResult addDependency(long blockingIssueId, long blockedIssueId, String currentLoginId) {
        currentLoginId = requireText(currentLoginId, "currentLoginId");
        blockedIssueId = requirePositive(blockedIssueId, "blockedIssueId");
        blockingIssueId = requirePositive(blockingIssueId, "blockingIssueId");
        Issue blockingIssue = findIssue(blockingIssueId);
        Issue blockedIssue = findIssue(blockedIssueId);
        requireNotDeleted(blockingIssue);
        requireNotDeleted(blockedIssue);
        requireDependencyAddStatus(blockedIssue);
        requireSameProjectDependency(blockingIssue, blockedIssue);
        User actor = findUser(currentLoginId);
        permissionPolicy.assertCanManageDependency(actor, blockedIssue);
        requireProjectLead(actor, blockedIssue.projectId(), "Only the project PL can manage dependencies.");
        validateDependency(blockingIssueId, blockedIssueId);
        LocalDateTime now = now();
        String dependencyId = IssueDependency.dependencyIdFor(blockingIssueId, blockedIssueId);
        IssueDependency dependency = blockedIssue.addDependency(dependencyId, blockingIssue, actor, now);
        IssueDependency saved = dependencyRepository.saveAndRecordIssueChange(dependency, blockedIssue);
        return toDependencyResult(saved, blockingIssue, blockedIssue);
    }

    public List<DependencyResult> viewProjectDependencies(long projectId, String currentLoginId) {
        projectId = requirePositive(projectId, "projectId");
        currentLoginId = requireText(currentLoginId, "currenLoginId");
        findProject(projectId);
        User actor = findUser(currentLoginId);
        permissionPolicy.assertCanViewIssue(actor);
        requireActiveProjectMember(actor, projectId, "Only project members can view project dependencies.");
        List<IssueDependency> deps = dependencyRepository.findByProjectId(projectId);
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
        currentLoginId = requireText(currentLoginId, "currentLoginId");
        long requiredBlockedIssueId = requirePositive(blockedIssueId, "blockedIssueId");
        long requiredBlockingIssueId = requirePositive(blockingIssueId, "blockingIssueId");
        String hashedDependencyId = IssueDependency.dependencyIdFor(requiredBlockingIssueId, requiredBlockedIssueId);
        IssueDependency dependency = dependencyRepository.findByDependencyId(hashedDependencyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Dependency not found: " + requiredBlockingIssueId + " -> " + requiredBlockedIssueId));
        Issue blockingIssue = findIssue(dependency.blockingIssueId());
        Issue blockedIssue = findIssue(dependency.blockedIssueId());
        requireNotDeleted(blockingIssue);
        requireNotDeleted(blockedIssue);
        User actor = findUser(currentLoginId);
        permissionPolicy.assertCanManageDependency(actor, blockedIssue);
        requireProjectLead(actor, blockedIssue.projectId(), "Only the project PL can manage dependencies.");
        requireSameProjectDependency(blockingIssue, blockedIssue);
        blockedIssue.removeDependency(dependency, actor, now());
        dependencyRepository.deleteByDependencyIdAndRecordIssueChange(hashedDependencyId, blockedIssue);
    }

    public void deleteComment(long issueId, long commentId, String currentLoginId) {
        issueId = requirePositive(issueId, "issueId");
        commentId = requirePositive(commentId, "commentId");
        currentLoginId = requireText(currentLoginId, "currentLoginId");
        Issue issue = findIssue(issueId);
        requireNotDeleted(issue);
        Comment comment = findComment(commentId);
        User currentUser = findUser(currentLoginId);
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

    // 여기서부터 ㅅㅂ 다시봐야함
    public CommentResult updateComment(long issueId, long commentId, String content, String currentLoginId) {
        Issue issue = findIssue(issueId);
        requireNotDeleted(issue);
        Comment comment = findComment(commentId);
        User currentUser = findUser(currentLoginId);
        requireCommentBelongsToIssue(comment, issue);
        permissionPolicy.assertCanUpdateComment(currentUser, comment);
        requireActiveProjectMember(currentUser, issue.projectId(), "Only project members can update issue comments.");

        String previousContent = comment.content();
        LocalDateTime changedAt = now();
        comment.changeContent(content, changedAt);
        IssueHistory history = IssueHistory.newForPersistence(
                issue.id(),
                currentUser.getLoginId(),
                ActionType.COMMENTED,
                previousContent,
                comment.content(),
                comment.content(),
                changedAt);
        Comment saved = commentRepository.saveAndRecordIssueChange(comment, history);
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
        boolean projectLead = userRepository.findActiveByRole(projectId, Role.PL).stream()
                .anyMatch(user -> user.getLoginId().equals(actor.getLoginId()));
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
        return userRepository.findActiveByRole(projectId, actor.getRole()).stream()
                .anyMatch(user -> user.getLoginId().equals(actor.getLoginId()));
    }

    private static boolean isRelatedParticipant(Issue issue, String loginId) {
        return loginId.equals(issue.reporterId())
                || loginId.equals(issue.assigneeId())
                || loginId.equals(issue.verifierId());
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

    private static void requireDependencyAddStatus(Issue blockedIssue) {
        if (blockedIssue.status() == IssueStatus.RESOLVED || blockedIssue.status() == IssueStatus.CLOSED) {
            throw new IllegalStateException("Resolved or closed issues cannot be blocked by a new dependency.");
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
            for (var dep : dependencyRepository.findByBlockedIssueId(current)) {
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
            List<DependencyResult> dependencies) {
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
                dependencies);
    }

    private static CommentResult toCommentResult(Comment comment) {
        return new CommentResult(
                comment.getCommentId(),
                comment.getContent(),
                comment.getPurpose(),
                comment.writerId(),
                toUserResult(comment.getWriter()),
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
