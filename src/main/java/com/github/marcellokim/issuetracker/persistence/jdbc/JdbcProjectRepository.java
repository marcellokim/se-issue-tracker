package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcProjectRepository implements ProjectRepository {

    private final DatabaseConnectionProvider connectionProvider;

    public JdbcProjectRepository(DatabaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<Project> findById(long projectId) {
        String sql = """
                select id, name, description, managed_by_login_id, created_at, updated_at
                from projects
                where id = ?
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapProject(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to find project by id.", exception);
        }
    }

    @Override
    public Optional<Project> findByName(String name) {
        String sql = """
                select id, name, description, managed_by_login_id, created_at, updated_at
                from projects
                where name = ?
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapProject(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to find project by name.", exception);
        }
    }

    @Override
    public Project save(Project project) {
        if (project.getId() == 0L) {
            return insert(project);
        }
        return update(project);
    }

    @Override
    public void deleteById(long projectId) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                deleteProjectMembers(connection, projectId);
                deleteProjectIssues(connection, projectId);
                deleteProject(connection, projectId);
                connection.commit();
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                connection.setAutoCommit(originalAutoCommit);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to delete project.", exception);
        }
    }

    @Override
    public void addParticipant(long projectId, String userId) {
        String sql = """
                merge into project_members target
                using (select ? as project_id, ? as user_login_id from dual) source
                on (target.project_id = source.project_id and target.user_login_id =
                source.user_login_id)
                when not matched then
                insert (project_id, user_login_id, joined_at)
                values (source.project_id, source.user_login_id, current_timestamp)
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            statement.setString(2, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to add project participant.",
                    exception);
        }
    }

    @Override
    public void removeParticipant(long projectId, String userId) {
        String sql = "delete from project_members where project_id = ? and user_login_id = ?";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            statement.setString(2, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to remove project participant.", exception);
        }
    }

    @Override
    public List<ProjectMember> findParticipants(long projectId) {
        String sql = """
                select project_id, user_login_id, joined_at
                from project_members
                where project_id = ?
                order by user_login_id
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ProjectMember> members = new ArrayList<>();
                while (resultSet.next()) {
                    members.add(ProjectMember.fromPersistence(
                            resultSet.getLong("project_id"),
                            resultSet.getString("user_login_id"),
                            JdbcSupport.nullableDateTime(resultSet, "joined_at")));
                }
                return members;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to list project participants.",
                    exception);
        }
    }

    @Override
    public boolean existsByParticipant(String userLoginId) {
        String sql = """
                select 1
                from project_members
                where user_login_id = ?
                  and rownum = 1
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userLoginId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to check project participant existence.", exception);
        }
    }

    private Project insert(Project project) {
        String sql = """
                insert into projects (name, description, managed_by_login_id, created_at,
                updated_at)
                values (?, ?, ?, coalesce(?, current_timestamp), coalesce(?,
                current_timestamp))
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = JdbcSupport.prepareInsertReturningId(connection, sql)) {
            statement.setString(1, project.getName());
            JdbcSupport.setNullableString(statement, 2, project.getDescription());
            statement.setString(3, project.getManagedByLoginId());
            JdbcSupport.setNullableTimestamp(statement, 4, project.getCreatedDate());
            JdbcSupport.setNullableTimestamp(statement, 5, project.getUpdatedAt());
            statement.executeUpdate();
            return findById(JdbcSupport.generatedId(statement))
                    .orElseThrow(() -> new RepositoryException("Inserted project was not found.",
                            null));
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to insert project.", exception);
        }
    }

    private Project update(Project project) {
        String sql = """
                update projects
                set name = ?,
                description = ?,
                managed_by_login_id = ?,
                updated_at = coalesce(?, current_timestamp)
                where id = ?
                """;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, project.getName());
            JdbcSupport.setNullableString(statement, 2, project.getDescription());
            statement.setString(3, project.getManagedByLoginId());
            JdbcSupport.setNullableTimestamp(statement, 4, project.getUpdatedAt());
            statement.setLong(5, project.getId());
            statement.executeUpdate();
            return findById(project.getId())
                    .orElseThrow(() -> new RepositoryException("Updated project was not found.",
                            null));
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to update project.", exception);
        }
    }

    private static void deleteProjectMembers(Connection connection, long projectId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "delete from project_members where project_id = ?")) {
            statement.setLong(1, projectId);
            statement.executeUpdate();
        }
    }

    private static void deleteProjectIssues(Connection connection, long projectId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "delete from issues where project_id = ?")) {
            statement.setLong(1, projectId);
            statement.executeUpdate();
        }
    }

    private static void deleteProject(Connection connection, long projectId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "delete from projects where id = ?")) {
            statement.setLong(1, projectId);
            statement.executeUpdate();
        }
    }

    private static void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // 원래 repository 실패 원인 유지.
        }
    }

    static Project mapProject(ResultSet resultSet) throws SQLException {
        return Project.fromPersistence(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getString("managed_by_login_id"),
                JdbcSupport.nullableDateTime(resultSet, "created_at"),
                JdbcSupport.nullableDateTime(resultSet, "updated_at"));
    }
}
