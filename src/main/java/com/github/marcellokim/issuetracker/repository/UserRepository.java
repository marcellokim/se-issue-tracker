package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    /**
     * Finds a user by the domain identity. In the current Oracle schema, userId is
     * persisted in USERS.LOGIN_ID because login_id is the user primary key.
     */
    Optional<User> findById(String userId);

    Optional<User> findByLoginId(String loginId);

    List<User> findAll();

    List<User> findActiveByRole(long projectId, Role role);

    User save(User user);

    void deactivate(String loginId);
}
