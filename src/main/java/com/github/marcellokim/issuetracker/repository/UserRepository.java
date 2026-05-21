package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    /**
     * 도메인 식별자로 사용자 조회.
     * 현재 Oracle schema에서는 login_id가 사용자 primary key라 userId도 USERS.LOGIN_ID에 저장됨.
     */
    Optional<User> findById(String userId);

    Optional<User> findByLoginId(String loginId);

    List<User> findAll();

    List<User> findActiveByRole(long projectId, Role role);

    User save(User user);

    void deactivate(String loginId);
}
