package com.github.marcellokim.issuetracker.persistence.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.repository.RepositoryException;

public final class JdbcAssignmentRecommendationRepository implements AssignmentRecommendationRepository {

    private static final String FIND_RESOLVED_ISSUES_SQL = """
            select title, description, fixer_login_id, resolver_login_id
            from issues
            where project_id = ? and status in ('RESOLVED', 'CLOSED')
            order by id
            """;
    private static final String FIND_ACTIVE_CANDIDATES_SQL = """
            select u.login_id, u.name, u.role, u.active, u.created_at, u.updated_at
            from users u
            join project_members pm on pm.user_login_id = u.login_id
            where pm.project_id = ? and u.role = ? and u.active = 1
            order by u.login_id
            """;

    private final DatabaseConnectionProvider connectionProvider;
    public JdbcAssignmentRecommendationRepository(DatabaseConnectionProvider connectionProvider){
        this.connectionProvider = connectionProvider;
    }

    @Override
    public List<IssueRecommendationData> findResolvedIssuesForRecommendation(long projectId){
        try(Connection connection = connectionProvider.getConnection();
            PreparedStatement statement = connection.prepareStatement(FIND_RESOLVED_ISSUES_SQL)){
            statement.setLong(1, projectId);
            List<IssueRecommendationData> searchedResolvedIssues = new ArrayList<>();
            try(ResultSet resultSet = statement.executeQuery()){
                while (resultSet.next()){
                    searchedResolvedIssues.add(new IssueRecommendationData(
                        resultSet.getString("title"),
                        resultSet.getString("description"),
                        resultSet.getString("fixer_login_id"),
                        resultSet.getString("resolver_login_id")));
                }
            }
            return searchedResolvedIssues;
        } catch (SQLException exception){
            throw new RepositoryException("Failed to find issues for recommendation - SQL fault", exception);
        }
    }

    @Override
    public List<User> findActiveDevCandidates(long projectId){
        return findActiveCandidatesByRole(projectId, Role.DEV);
    }
    @Override
    public List<User> findActiveTesterCandidates(long projectId){
        return findActiveCandidatesByRole(projectId, Role.TESTER);
    }
    private List<User> findActiveCandidatesByRole(long projectId, Role targetRole){
        try(Connection connection = connectionProvider.getConnection();
            PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE_CANDIDATES_SQL)){
            statement.setLong(1, projectId);
            statement.setString(2, targetRole.name());
            List<User> activeCandidateUsers = new ArrayList<>();
            try(ResultSet resultSet = statement.executeQuery()){
                while (resultSet.next()){ activeCandidateUsers.add(mapCandidateUser(resultSet)); }
            }
            return activeCandidateUsers;
        } catch (SQLException exception){
            throw new RepositoryException("Failed to find active candidates - SQL fault", exception);
        }
    }
    private static User mapCandidateUser(ResultSet resultSet) throws SQLException {
        return User.fromPersistence(
            resultSet.getString("login_id"),
            resultSet.getString("name"),
            "not-loaded",
            Role.valueOf(resultSet.getString("role")),
            resultSet.getInt("active") == 1,
            JdbcSupport.nullableDateTime(resultSet, "created_at"),
            JdbcSupport.nullableDateTime(resultSet, "updated_at"));
    }
}
