package com.github.marcellokim.issuetracker.persistence.jdbc;

import com.github.marcellokim.issuetracker.domain.AssignmentCandidate;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcAssignmentRecommendationRepository implements AssignmentRecommendationRepository {

    private final DatabaseConnectionProvider connectionProvider;

    public JdbcAssignmentRecommendationRepository(DatabaseConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public List<AssignmentCandidate> findDevAssigneeCandidates(long projectId) {
        return findCandidates(projectId, Role.DEV, "fixer_login_id");
    }

    @Override
    public List<AssignmentCandidate> findTesterVerifierCandidates(long projectId) {
        return findCandidates(projectId, Role.TESTER, "resolver_login_id");
    }

    private List<AssignmentCandidate> findCandidates(long projectId, Role role, String issueUserColumn) {
        String sql = """
                select u.login_id,
                       coalesce(c.password_salt || ':' || c.password_hash, u.password) as password,
                       u.role,
                       u.active,
                       u.created_at,
                       u.updated_at,
                       count(i.id) as completed_issue_count
                from users u
                left join user_credentials c on c.login_id = u.login_id
                join project_members pm on pm.user_login_id = u.login_id
                left join issues i
                  on i.project_id = pm.project_id
                 and i.%s = u.login_id
                 and i.status in ('RESOLVED', 'CLOSED')
                where pm.project_id = ?
                  and u.role = ?
                  and u.active = 1
                group by u.login_id,
                         coalesce(c.password_salt || ':' || c.password_hash, u.password),
                         u.role,
                         u.active,
                         u.created_at,
                         u.updated_at
                order by completed_issue_count desc, u.login_id
                """.formatted(issueUserColumn);

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            statement.setString(2, role.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AssignmentCandidate> candidates = new ArrayList<>();
                while (resultSet.next()) {
                    User user = JdbcUserRepository.mapUser(resultSet);
                    candidates.add(new AssignmentCandidate(user, resultSet.getInt("completed_issue_count")));
                }
                return candidates;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to find assignment candidates.", exception);
        }
    }
}
