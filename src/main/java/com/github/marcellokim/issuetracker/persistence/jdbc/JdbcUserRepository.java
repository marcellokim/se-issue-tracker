package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcUserRepository implements UserRepository {

    private static final String BASE_SELECT = """
            select u.login_id,
                   u.name,
                   c.password_salt || ':' || c.password_hash as password,
                   u.role,
                   u.active,
                   u.created_at,
                   u.updated_at
            from users u
            join user_credentials c on c.login_id = u.login_id
            """;
    private static final String FIND_BY_LOGIN_ID_SQL = BASE_SELECT + " where u.login_id = ?";
    private static final String FIND_ALL_SQL = BASE_SELECT + " order by u.login_id";
    private static final String FIND_ACTIVE_BY_ROLE_SQL = """
            select u.login_id,
                   u.name,
                   c.password_salt || ':' || c.password_hash as password,
                   u.role,
                   u.active,
                   u.created_at,
                   u.updated_at
            from users u
            join user_credentials c on c.login_id = u.login_id
            join project_members pm on pm.user_login_id = u.login_id
            where pm.project_id = ?
              and u.role = ?
              and u.active = 1
            order by u.login_id
            """;
    private static final String INSERT_USER_SQL = """
            insert into users (login_id, name, role, active, created_at, updated_at)
            values (?, ?, ?, ?, coalesce(?, current_timestamp), coalesce(?, current_timestamp))
            """;
    private static final String UPDATE_USER_SQL = """
            update users
            set name = ?,
                role = ?,
                active = ?,
                updated_at = coalesce(?, current_timestamp)
            where login_id = ?
            """;
    private static final String UPSERT_CREDENTIAL_SQL = """
            merge into user_credentials target
            using (
                select ? as login_id, ? as password_salt, ? as password_hash from dual
            ) source on (target.login_id = source.login_id)
            when matched then update
            set target.password_salt = source.password_salt,
                target.password_hash = source.password_hash,
                target.updated_at = current_timestamp
            when not matched then
            insert (login_id, password_salt, password_hash)
            values (source.login_id, source.password_salt, source.password_hash)
            """;
    private static final String DEACTIVATE_SQL =
            "update users set active = 0, updated_at = current_timestamp where login_id = ?";

    private final DatabaseConnectionProvider connectionProvider;
    private final PasswordHasher passwordHasher;

    public JdbcUserRepository(DatabaseConnectionProvider connectionProvider) {
        this(connectionProvider, new PasswordHasher());
    }

    public JdbcUserRepository(DatabaseConnectionProvider connectionProvider, PasswordHasher passwordHasher) {
        this.connectionProvider = connectionProvider;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public Optional<User> findById(String userId) {
        return findByLoginId(userId);
    }

    @Override
    public Optional<User> findByLoginId(String loginId) {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_LOGIN_ID_SQL)) {
            statement.setString(1, loginId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to find user by login id.", exception);
        }
    }

    @Override
    public List<User> findAll() {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
            return users;
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list users.", exception);
        }
    }

    @Override
    public List<User> findActiveByRole(long projectId, Role role) {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE_BY_ROLE_SQL)) {
            statement.setLong(1, projectId);
            statement.setString(2, role.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }
                return users;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to find active users by role.", exception);
        }
    }

    @Override
    public User save(User user) {
        if (findByLoginId(user.getLoginId()).isPresent()) {
            return update(user);
        }
        return insert(user);
    }

    @Override
    public void deactivate(String loginId) {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(DEACTIVATE_SQL)) {
            statement.setString(1, loginId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to deactivate user.", exception);
        }
    }

    private User insert(User user) {
        String credential = normalizedCredential(user.getPasswordHash());
        return executeUserWrite(
                user.getLoginId(),
                connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(INSERT_USER_SQL)) {
                        bindUser(statement, user);
                        statement.executeUpdate();
                        upsertCredential(connection, user.getLoginId(), credential);
                    }
                },
                "Inserted user was not found.",
                "Failed to insert user."
        );
    }

    private User update(User user) {
        String credential = normalizedCredential(user.getPasswordHash());
        return executeUserWrite(
                user.getLoginId(),
                connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(UPDATE_USER_SQL)) {
                        bindUserForUpdate(statement, user);
                        statement.executeUpdate();
                        upsertCredential(connection, user.getLoginId(), credential);
                    }
                },
                "Updated user was not found.",
                "Failed to update user."
        );
    }

    private User executeUserWrite(
            String loginId,
            TransactionalUserWrite writeOperation,
            String notFoundMessage,
            String failureMessage
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                writeOperation.execute(connection);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                throw exception;
            } finally {
                restoreAutoCommit(connection, originalAutoCommit);
            }
            return findByLoginId(loginId)
                    .orElseThrow(() -> new RepositoryException(notFoundMessage, null));
        } catch (SQLException exception) {
            throw new RepositoryException(failureMessage, exception);
        }
    }

    private static void bindUser(PreparedStatement statement, User user) throws SQLException {
        statement.setString(1, user.getLoginId());
        statement.setString(2, user.getName());
        statement.setString(3, user.getRole().name());
        statement.setInt(4, user.isActive() ? 1 : 0);
        JdbcSupport.setNullableTimestamp(statement, 5, user.getCreatedAt());
        JdbcSupport.setNullableTimestamp(statement, 6, user.getUpdatedAt());
    }

    private static void bindUserForUpdate(PreparedStatement statement, User user) throws SQLException {
        statement.setString(1, user.getName());
        statement.setString(2, user.getRole().name());
        statement.setInt(3, user.isActive() ? 1 : 0);
        JdbcSupport.setNullableTimestamp(statement, 4, user.getUpdatedAt());
        statement.setString(5, user.getLoginId());
    }

    private void upsertCredential(Connection connection, String loginId, String credential) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_CREDENTIAL_SQL)) {
            statement.setString(1, loginId);
            statement.setString(2, passwordHasher.saltOf(credential));
            statement.setString(3, passwordHasher.hashOf(credential));
            statement.executeUpdate();
        }
    }

    private static void restoreAutoCommit(Connection connection, boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException ignored) {
            // 원래 repository 실패 원인을 유지한다.
        }
    }

    private static void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // 원래 repository 실패 원인을 유지한다.
        }
    }

    private String normalizedCredential(String passwordOrCredential) {
        if (passwordHasher.isHashed(passwordOrCredential)) {
            return passwordOrCredential;
        }
        return passwordHasher.hash(passwordOrCredential);
    }

    static User mapUser(ResultSet resultSet) throws SQLException {
        return User.create(
                resultSet.getString("login_id"),
                resultSet.getString("name"),
                resultSet.getString("password"),
                Role.valueOf(resultSet.getString("role")),
                resultSet.getInt("active") == 1,
                JdbcSupport.nullableDateTime(resultSet, "created_at"),
                JdbcSupport.nullableDateTime(resultSet, "updated_at")
        );
    }

    @FunctionalInterface
    private interface TransactionalUserWrite {

        void execute(Connection connection) throws SQLException;
    }
}
