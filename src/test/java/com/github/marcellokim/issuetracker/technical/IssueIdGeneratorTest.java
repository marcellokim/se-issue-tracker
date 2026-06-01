package com.github.marcellokim.issuetracker.technical;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("issue id generator")
class IssueIdGeneratorTest {

    @Test
    @DisplayName("generates prefixed issue ids")
    void generatesPrefixedIssueIds() {
        IssueIdGenerator generator = new IssueIdGenerator();

        String first = generator.nextIssueId();
        String second = generator.nextIssueId();

        assertTrue(first.startsWith("ISSUE-"));
        assertTrue(second.startsWith("ISSUE-"));
        assertNotEquals(first, second);
    }
}
