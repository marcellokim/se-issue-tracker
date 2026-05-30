package com.github.marcellokim.issuetracker.persistence.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.repository.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.support.IssueSearchCriteriaTestFactory;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JDBC issue search SQL")
class JdbcIssueQueriesTest {

    @Test
    @DisplayName("default search excludes deleted issues and preserves ordering")
    void defaultSearchExcludesDeletedIssues() {
        var query = JdbcIssueQueries.search(IssueSearchCriteriaTestFactory.all(10L));

        assertTrue(query.sql().contains("and i.project_id = ?"));
        assertTrue(query.sql().contains("and i.status <> 'DELETED'"));
        assertTrue(query.sql().endsWith(" order by i.reported_at desc, i.id desc"));
        assertEquals(1, query.binders().size());
    }

    @Test
    @DisplayName("explicit status does not add deleted exclusion twice")
    void explicitStatusControlsDeletedFilter() {
        var criteria = IssueSearchCriteria.create(
                10L,
                IssueStatus.DELETED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false);

        var query = JdbcIssueQueries.search(criteria);

        assertTrue(query.sql().contains("and i.status = ?"));
        assertFalse(query.sql().contains("and i.status <> 'DELETED'"));
        assertEquals(2, query.binders().size());
    }

    @Test
    @DisplayName("include deleted search does not add default deleted exclusion")
    void includeDeletedSearchDoesNotAddDeletedExclusion() {
        var criteria = IssueSearchCriteria.create(
                10L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true);

        var query = JdbcIssueQueries.search(criteria);

        assertFalse(query.sql().contains("and i.status <> 'DELETED'"));
        assertEquals(1, query.binders().size());
    }

    @Test
    @DisplayName("search query binder list is immutable")
    void searchQueryBinderListIsImmutable() {
        var query = JdbcIssueQueries.search(IssueSearchCriteriaTestFactory.all(10L));

        assertThrows(UnsupportedOperationException.class, () -> query.binders().clear());
    }

    @Test
    @DisplayName("all supported criteria keep binder order stable")
    void allCriteriaKeepBinderOrderStable() throws Exception {
        LocalDateTime reportedFrom = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime reportedTo = LocalDateTime.of(2026, 6, 1, 0, 0);
        var criteria = IssueSearchCriteria.create(
                10L,
                IssueStatus.NEW,
                Priority.MAJOR,
                "tester1",
                "dev1",
                "tester2",
                "Login",
                reportedFrom,
                reportedTo,
                false);

        var query = JdbcIssueQueries.search(criteria);
        var recorder = new PreparedStatementRecorder();
        PreparedStatement statement = recorder.preparedStatement();

        for (int index = 0; index < query.binders().size(); index++) {
            query.binders().get(index).bind(statement, index + 1);
        }

        assertEquals(10, query.binders().size());
        assertTrue(query.sql().contains("and i.project_id = ?"));
        assertTrue(query.sql().contains("and i.priority = ?"));
        assertTrue(query.sql().contains("lower(i.description) like ?"));
        assertEquals(List.of(
                new BoundValue(1, 10L),
                new BoundValue(2, "NEW"),
                new BoundValue(3, "MAJOR"),
                new BoundValue(4, "tester1"),
                new BoundValue(5, "dev1"),
                new BoundValue(6, "tester2"),
                new BoundValue(7, "%login%"),
                new BoundValue(8, "%login%"),
                new BoundValue(9, reportedFrom),
                new BoundValue(10, reportedTo)
        ), recorder.boundValues());
    }

    private record BoundValue(int index, Object value) {
    }

    private static final class PreparedStatementRecorder {

        private final List<BoundValue> boundValues = new ArrayList<>();

        PreparedStatement preparedStatement() {
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[] {PreparedStatement.class},
                    (proxy, method, arguments) -> {
                        if ("setLong".equals(method.getName())
                                || "setString".equals(method.getName())
                                || "setTimestamp".equals(method.getName())) {
                            Object value = arguments[1];
                            if (value instanceof Timestamp timestamp) {
                                value = timestamp.toLocalDateTime();
                            }
                            boundValues.add(new BoundValue((Integer) arguments[0], value));
                            return null;
                        }
                        if ("toString".equals(method.getName())) {
                            return "PreparedStatementRecorder";
                        }
                        throw new UnsupportedOperationException(
                                "Unexpected PreparedStatement call: " + method.getName());
                    });
        }

        List<BoundValue> boundValues() {
            return boundValues;
        }
    }
}
