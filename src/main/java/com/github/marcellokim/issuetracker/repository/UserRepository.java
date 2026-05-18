package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(String loginId);

    Optional<User> findByLoginId(String loginId);

    List<User> findAll();

    List<User> findActiveByRole(long projectId, Role role);

    User save(User user);

    void deactivate(String loginId);
}
