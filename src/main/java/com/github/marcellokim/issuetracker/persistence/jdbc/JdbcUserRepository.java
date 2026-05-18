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
    public Optional<User> findById(String loginId) {
        return findByLoginId(loginId);
    }

    @Override
    public Optional<User> findByLoginId(String loginId) {
        String sql = baseSelect() + " where u.login_id = ?";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
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
        String sql = baseSelect() + " order by u.login_id";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
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
        String sql = """
                select u.login_id,
                       coalesce(c.password_salt || ':' || c.password_hash, u.password) as password,
                       u.role,
                       u.active,
                       u.created_at,
                       u.updated_at
                from users u
                left join user_credentials c on c.login_id = u.login_id
                join project_members pm on pm.user_login_id = u.login_id
                where pm.project_id = ?
                  and u.role = ?
                  and u.active = 1
                order by u.login_id
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
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
        if (findByLoginId(user.loginId()).isPresent()) {
            return update(user);
        }
        return insert(user);
    }

    @Override
    public void deactivate(String loginId) {
        String sql = "update users set active = 0, updated_at = current_timestamp where login_id = ?";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, loginId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to deactivate user.", exception);
        }
    }

    private User insert(User user) {
        String credential = normalizedCredential(user.password());
        String sql = """
                insert into users (login_id, password, role, active, created_at, updated_at)
                values (?, ?, ?, ?, coalesce(?, current_timestamp), coalesce(?, current_timestamp))
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindUser(statement, user, credential);
            statement.executeUpdate();
            upsertCredential(connection, user.loginId(), credential);
            return findByLoginId(user.loginId())
                    .orElseThrow(() -> new RepositoryException("Inserted user was not found.", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to insert user.", exception);
        }
    }

    private User update(User user) {
        String credential = normalizedCredential(user.password());
        String sql = """
                update users
                set password = ?,
                    role = ?,
                    active = ?,
                    updated_at = coalesce(?, current_timestamp)
                where login_id = ?
        """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, credential);
            statement.setString(2, user.role().name());
            statement.setInt(3, user.active() ? 1 : 0);
            JdbcSupport.setNullableTimestamp(statement, 4, user.updatedAt());
            statement.setString(5, user.loginId());
            statement.executeUpdate();
            upsertCredential(connection, user.loginId(), credential);
            return findByLoginId(user.loginId())
                    .orElseThrow(() -> new RepositoryException("Updated user was not found.", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to update user.", exception);
        }
    }

    private static void bindUser(PreparedStatement statement, User user, String credential) throws SQLException {
        statement.setString(1, user.loginId());
        statement.setString(2, credential);
        statement.setString(3, user.role().name());
        statement.setInt(4, user.active() ? 1 : 0);
        JdbcSupport.setNullableTimestamp(statement, 5, user.createdAt());
        JdbcSupport.setNullableTimestamp(statement, 6, user.updatedAt());
    }

    private void upsertCredential(Connection connection, String loginId, String credential) throws SQLException {
        String sql = """
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
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, loginId);
            statement.setString(2, passwordHasher.saltOf(credential));
            statement.setString(3, passwordHasher.hashOf(credential));
            statement.executeUpdate();
        }
    }

    private String normalizedCredential(String passwordOrCredential) {
        if (passwordHasher.isHashed(passwordOrCredential)) {
            return passwordOrCredential;
        }
        return passwordHasher.hash(passwordOrCredential);
    }

    static User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getString("login_id"),
                resultSet.getString("password"),
                Role.valueOf(resultSet.getString("role")),
                resultSet.getInt("active") == 1,
                JdbcSupport.nullableDateTime(resultSet, "created_at"),
                JdbcSupport.nullableDateTime(resultSet, "updated_at")
        );
    }

    private static String baseSelect() {
        return """
                select u.login_id,
                       coalesce(c.password_salt || ':' || c.password_hash, u.password) as password,
                       u.role,
                       u.active,
                       u.created_at,
                       u.updated_at
                from users u
                left join user_credentials c on c.login_id = u.login_id
                """;
    }
}
