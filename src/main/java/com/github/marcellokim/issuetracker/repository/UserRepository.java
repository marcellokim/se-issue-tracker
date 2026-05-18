package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(String userId);

    List<User> findActiveDevelopers();

    List<User> findActiveTesters();
}
