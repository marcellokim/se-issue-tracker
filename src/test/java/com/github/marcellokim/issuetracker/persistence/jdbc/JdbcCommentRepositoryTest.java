package com.github.marcellokim.issuetracker.persistence.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JDBC comment repository")
class JdbcCommentRepositoryTest {

    @Test
    @DisplayName("legacy comments without purpose are treated as general comments")
    void mapCommentDefaultsMissingPurposeToGeneral() throws Exception {
        var createdDate = LocalDateTime.of(2026, 5, 21, 12, 0);
        var updatedDate = createdDate.plusHours(2);
        var row = resultSet(Map.of(
                "id", 10L,
                "issue_id", 20L,
                "writer_login_id", "dev1",
                "content", "legacy comment",
                "created_at", Timestamp.valueOf(createdDate),
                "updated_at", Timestamp.valueOf(updatedDate)
        ));

        var comment = JdbcCommentRepository.mapComment(row);

        assertEquals(CommentPurpose.GENERAL, comment.getPurpose());
        assertEquals(createdDate, comment.getCreatedDate());
        assertEquals(updatedDate, comment.getUpdatedDate());
    }

    private static ResultSet resultSet(Map<String, Object> values) {
        return (ResultSet) Proxy.newProxyInstance(
                JdbcCommentRepositoryTest.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                (proxy, method, args) -> {
                    var name = method.getName();
                    if ("getLong".equals(name)) {
                        return ((Number) values.get((String) args[0])).longValue();
                    }
                    if ("getString".equals(name)) {
                        return (String) values.get((String) args[0]);
                    }
                    if ("getTimestamp".equals(name)) {
                        return (Timestamp) values.get((String) args[0]);
                    }
                    throw new UnsupportedOperationException(name);
                }
        );
    }
}
