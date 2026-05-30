package com.github.marcellokim.issuetracker.service;

import java.util.Objects;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.time.LocalDateTime;

public final class AccountService {

    private static final String FIELD_LOGIN_ID = "loginId";
    private static final String ROLE_REQUIRED = "role must not be null";
    private final PermissionPolicy permissionPolicy;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final PasswordHashing passwordHashing;
    private final Clock clock;

    public AccountService(
            PermissionPolicy permissionPolicy,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            IssueRepository issueRepository,
            PasswordHashing passwordHashing,
            Clock clock) {
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.passwordHashing = Objects.requireNonNull(passwordHashing, "passwordHashing");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public UserResult createAccount(
            String loginId,
            String name,
            String password,
            Role role,
            User actor) {
        loginId = requireText(loginId, FIELD_LOGIN_ID);
        name = requireText(name, "name");
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
        requireActor(actor);
        permissionPolicy.assertCanManageAccount(actor);
        Role newRole = Objects.requireNonNull(role, ROLE_REQUIRED);
        requireNonAdminAccount(loginId, newRole);
        if (userRepository.findByLoginId(loginId).isPresent()) {
            throw new IllegalArgumentException("Account already exists: " + loginId);
        }

        LocalDateTime now = clock.now();
        User user = User.create(
                loginId,
                name,
                passwordHashing.hash(password),
                newRole,
                now);
        return UserResult.from(userRepository.save(user));
    }

    public UserResult renameAccount(String loginId, String name, User actor) {
        loginId = requireText(loginId, FIELD_LOGIN_ID);
        name = requireText(name, "name");
        requireActor(actor);
        permissionPolicy.assertCanManageAccount(actor);
        requireDifferentAccount(loginId, actor.getLoginId());
        User target = findUser(loginId);
        requireNonAdminTarget(target);
        if (Objects.equals(target.getName(), name)) {
            throw new IllegalArgumentException("name is same with current name.");
        }
        target.rename(name, clock.now());
        return UserResult.from(userRepository.save(target));
    }

    public UserResult changeAccountRole(String loginId, Role role, User actor) {
        loginId = requireText(loginId, FIELD_LOGIN_ID);
        requireActor(actor);
        permissionPolicy.assertCanManageAccount(actor);
        requireDifferentAccount(loginId, actor.getLoginId());
        User target = findUser(loginId);
        Role newRole = Objects.requireNonNull(role, ROLE_REQUIRED);
        requireNonAdminTarget(target);
        requireNonAdminAccount(loginId, newRole);
        rejectRoleChangeWithProjectResponsibility(target, newRole);
        if (target.getRole() == newRole) {
            throw new IllegalArgumentException("Role is already same.");
        }
        target.changeRole(newRole, clock.now());
        return UserResult.from(userRepository.save(target));
    }

    public UserResult activateAccount(String loginId, User actor) {
        loginId = requireText(loginId, FIELD_LOGIN_ID);
        requireActor(actor);
        permissionPolicy.assertCanManageAccount(actor);
        requireDifferentAccount(loginId, actor.getLoginId());
        User target = findUser(loginId);
        requireNonAdminTarget(target);
        if (target.isActive()) {
            throw new IllegalArgumentException("Account is already active.");
        }
        target.activate(clock.now());
        return UserResult.from(userRepository.save(target));
    }

    public UserResult deactivateAccount(String loginId, User actor) {
        loginId = requireText(loginId, FIELD_LOGIN_ID);
        requireActor(actor);
        permissionPolicy.assertCanManageAccount(actor);
        requireDifferentAccount(loginId, actor.getLoginId());
        User target = findUser(loginId);
        requireNonAdminTarget(target);
        rejectDeactivationWithProjectResponsibility(target);
        if (!target.isActive()) {
            throw new IllegalArgumentException("Account is already inactive.");
        }
        target.deactivate(clock.now());
        return UserResult.from(userRepository.save(target));
    }

    private static void requireNonAdminAccount(String loginId, Role role) {
        if (isAdminLoginId(loginId) || role == Role.ADMIN) {
            throw new IllegalArgumentException("The admin account cannot be created or modified.");
        }
    }

    private static void requireNonAdminTarget(User target) {
        if (target.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("The admin account cannot be created or modified.");
        }
    }

    private static void requireDifferentAccount(String targetLoginId, String currentUserId) {
        if (Objects.equals(targetLoginId, currentUserId)) {
            throw new IllegalArgumentException("Admin must manage another account.");
        }
    }

    private void rejectRoleChangeWithProjectResponsibility(User target, Role newRole) {
        if (target.getRole() == newRole) {
            return;
        }
        if (projectRepository.existsByParticipant(target.getLoginId())) {
            throw new IllegalArgumentException(
                    "Account role can be changed only when the user has no project membership.");
        }
        if (issueRepository.hasCurrentIssueResponsibility(target.getLoginId())) {
            throw new IllegalArgumentException(
                    "Account role can be changed only when the user has no assigned issue responsibility.");
        }
    }

    private void rejectDeactivationWithProjectResponsibility(User target) {
        if (projectRepository.existsByParticipant(target.getLoginId())) {
            throw new IllegalArgumentException(
                    "Account can be deactivated only when the user has no project membership.");
        }
        if (issueRepository.hasCurrentIssueResponsibility(target.getLoginId())) {
            throw new IllegalArgumentException(
                    "Account can be deactivated only when the user has no assigned issue responsibility.");
        }
    }

    private static boolean isAdminLoginId(String loginId) {
        return loginId != null && "admin".equalsIgnoreCase(loginId.trim());
    }

    private static void requireActor(User actor) {
        Objects.requireNonNull(actor, "actor must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private User findUser(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + loginId));
    }
}
