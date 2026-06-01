package com.github.marcellokim.issuetracker.technical;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Comment id generator")
class CommentIdGeneratorTest {

    @Test
    @DisplayName("new comment ids are ready to save")
    void makesUsableCommentIds() {
        CommentIdGenerator generator = new CommentIdGenerator();

        String first = generator.nextCommentId();
        String second = generator.nextCommentId();

        assertTrue(first.startsWith("COMMENT-"));
        assertTrue(second.startsWith("COMMENT-"));
        assertNotEquals(first, second);
    }
}
