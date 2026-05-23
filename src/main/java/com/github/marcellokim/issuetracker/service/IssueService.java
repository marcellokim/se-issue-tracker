package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.repository.IssueDependencyRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public final class IssueService {

    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final IssueDependencyRepository dependencyRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PermissionPolicy permissionPolicy;
    private final Clock clock;

    public IssueService(
            ProjectRepository projectRepository,
            IssueRepository issueRepository,
            IssueDependencyRepository dependencyRepository,
            CommentRepository commentRepository,
            UserRepository userRepository,
            PermissionPolicy permissionPolicy,
            Clock clock
    ) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.dependencyRepository = Objects.requireNonNull(dependencyRepository, "dependencyRepository");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public IssueResult registerIssue(long projectId, String title, String description, Priority priority, String currentUserId) {
        Project project = findProject(projectId);
        User reporter = findUser(currentUserId);
        permissionPolicy.assertCanRegisterIssue(reporter, project);
        LocalDateTime now = now();
        Issue issue = Issue.newForPersistence(
                Issue.persistedState(project.getId(), title, description, reporter)
                        .priority(priority != null ? priority : Priority.MAJOR)
                        .reportedDate(now)
                        .updatedAt(now)
        );
        Issue saved = issueRepository.save(issue);
        return toIssueResult(saved);
    }

    public CommentResult addComment(long issueId, String content, String currentUserId) {
        Issue issue = findIssue(issueId);
        User writer = findUser(currentUserId);
        permissionPolicy.assertCanAddComment(writer, issue);
        LocalDateTime now = now();
        Comment comment = issue.addComment(CommentIdGenerator.nextCommentId(), content, writer, now);
        issueRepository.save(issue);
        return toCommentResult(comment);
    }

    public DependencyResult addDependency(long blockingIssueId, long blockedIssueId, String currentUserId) {
        Issue blockingIssue = findIssue(blockingIssueId);
        Issue blockedIssue = findIssue(blockedIssueId);
        User actor = findUser(currentUserId);
        permissionPolicy.assertCanManageDependency(actor, blockedIssue);
        requireProjectLead(actor, blockedIssue.projectId(), "Only the project PL can manage dependencies.");
        if (blockingIssue.projectId() != blockedIssue.projectId()) {
            requireProjectLead(actor, blockingIssue.projectId(), "Only the project PL can manage dependencies.");
        }
        validateDependency(blockingIssueId, blockedIssueId);
        LocalDateTime now = now();
        String dependencyId = IssueDependency.dependencyIdFor(blockingIssueId, blockedIssueId);
        IssueDependency dependency = blockedIssue.addDependency(dependencyId, blockingIssue, actor, now);
        IssueDependency saved = dependencyRepository.save(dependency);
        issueRepository.save(blockedIssue);
        return toDependencyResult(saved, blockingIssue, blockedIssue);
    }

    public void removeDependency(long dependencyId, String currentUserId) {
        IssueDependency dependency = dependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new IllegalArgumentException("Dependency not found: " + dependencyId));
        Issue blockedIssue = findIssue(dependency.blockedIssueId());
        User actor = findUser(currentUserId);
        permissionPolicy.assertCanManageDependency(actor, blockedIssue);
        requireProjectLead(actor, blockedIssue.projectId(), "Only the project PL can manage dependencies.");
        blockedIssue.removeDependency(dependency, actor, now());
        dependencyRepository.deleteById(dependencyId);
        issueRepository.save(blockedIssue);
    }

    public void deleteComment(long issueId, long commentId, String currentUserId) {
        Issue issue = findIssue(issueId);
        Comment comment = findComment(commentId);
        User currentUser = findUser(currentUserId);
        requireCommentBelongsToIssue(comment, issue);
        requireCommentWriter(comment, currentUser);
        requireGeneralComment(comment);

        issue.recordCommentDeletion(comment, currentUser, now());
        issueRepository.save(issue);
        commentRepository.deleteGeneralById(issue.id(), comment.id(), currentUser.getLoginId());
    }

    private Project findProject(long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
    }

    private Issue findIssue(long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
    }

    private Comment findComment(long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private void requireProjectLead(User actor, long projectId, String message) {
        boolean projectLead = userRepository.findActiveByRole(projectId, Role.PL).stream()
                .anyMatch(user -> user.getLoginId().equals(actor.getLoginId()));
        if (!projectLead) {
            throw new SecurityException(message);
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

    private static void requireCommentWriter(Comment comment, User currentUser) {
        if (!comment.writerId().equals(currentUser.getLoginId())) {
            throw new SecurityException("Only the comment writer can delete the comment.");
        }
    }

    private static void requireGeneralComment(Comment comment) {
        if (comment.purpose() != CommentPurpose.GENERAL) {
            throw new IllegalArgumentException("Only GENERAL comments can be deleted.");
        }
    }

    private LocalDateTime now() {
        return clock.now();
    }

    private static IssueResult toIssueResult(Issue issue) {
        return new IssueResult(
                issue.getIssueId(),
                issue.status(),
                issue.priority(),
                issue.title(),
                issue.description(),
                issue.getReporter()
        );
    }

    private static CommentResult toCommentResult(Comment comment) {
        return new CommentResult(
                comment.getCommentId(),
                comment.getContent(),
                comment.getPurpose(),
                comment.getWriter(),
                comment.getCreatedDate(),
                comment.getUpdatedDate()
        );
    }

    private static DependencyResult toDependencyResult(IssueDependency dep, Issue blockingIssue, Issue blockedIssue) {
        return new DependencyResult(
                dep.id(),
                dep.getDependencyId(),
                blockingIssue.getIssueId(),
                blockedIssue.getIssueId(),
                dep.getDiscoveredDate()
        );
    }
}
