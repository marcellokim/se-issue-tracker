package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.util.Objects;

public final class LoginCheckService {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    public LoginCheckService(UserRepository userRepository, AuthenticationService authenticationService) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
    }

    public LoginCheckResult checkLogin(String loginId, String password) {
        String normalizedLoginId = loginId == null ? "" : loginId.trim();
        AuthenticationResult authenticationResult = authenticationService.login(normalizedLoginId, password);
        return new LoginCheckResult(
                normalizedLoginId,
                userRepository.findByLoginId(normalizedLoginId).map(LoginCheckService::toAccountSummary),
                authenticationResult.success(),
                authenticationResult.message());
    }

    private static LoginCheckResult.AccountSummary toAccountSummary(User user) {
        return new LoginCheckResult.AccountSummary(user.getRole(), user.isActive());
    }
}
