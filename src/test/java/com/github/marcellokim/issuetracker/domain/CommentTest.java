package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Comment")
class CommentTest {

        private final User writer = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, true, null, null);
        private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

        @Test
        @DisplayName("new comment contains writer information")
        void containWriterInformation() {
                var comment = Comment.create(
                                "C-2", "Please verify again.", writer, CommentPurpose.GENERAL,
                                createdAt.plusMinutes(15));

                assertEquals(0L, comment.id());
                assertEquals(0L, comment.issueId());
                assertEquals("C-2", comment.getCommentId());
                assertEquals("dev1", comment.writerId());
                assertEquals("Please verify again.", comment.content());
                assertEquals(CommentPurpose.GENERAL, comment.getPurpose());
                assertSame(writer, comment.getWriter());
                assertEquals(createdAt.plusMinutes(15), comment.createdDate());
                assertEquals(createdAt.plusMinutes(15), comment.updatedDate());
        }

        @Test
        @DisplayName("comment can be attached to an issue")
        void attachCommentToIssue() {
                var comment = Comment.newForIssue(
                                100L, "I will check it.", writer, CommentPurpose.GENERAL, createdAt);

                assertEquals(0L, comment.id());
                assertEquals(100L, comment.issueId());
                assertEquals("NEW-COMMENT", comment.getCommentId());
                assertEquals("dev1", comment.writerId());
                assertSame(writer, comment.getWriter());
                assertEquals(createdAt, comment.getCreatedDate());
                assertEquals(createdAt, comment.getUpdatedDate());
        }

        @Test
        @DisplayName("loaded comment matches saved data")
        void loadedCommentMatchesSavedData() {
                var updatedAt = createdAt.plusHours(2);
                var comment = Comment.fromPersistence(
                                11L, 100L, "dev1", "I fixed this.", CommentPurpose.GENERAL, createdAt, updatedAt);

                assertEquals(11L, comment.id());
                assertEquals(100L, comment.issueId());
                assertEquals("11", comment.getCommentId());
                assertEquals("dev1", comment.writerId());
                assertEquals("I fixed this.", comment.content());
                assertEquals(CommentPurpose.GENERAL, comment.getPurpose());
                assertEquals(createdAt, comment.createdDate());
                assertEquals(updatedAt, comment.updatedDate());
                assertNull(comment.getWriter());
        }

        @Test
        @DisplayName("comment text can be changed")
        void changesCommentText() {
                var comment = Comment.create(
                                "C-2", "Please verify again.", writer, CommentPurpose.GENERAL, createdAt);
                var changedAt = createdAt.plusHours(3);

                comment.changeContent("Please verify the regression case.", changedAt);

                assertEquals("Please verify the regression case.", comment.content());
                assertEquals(createdAt, comment.getCreatedDate());
                assertEquals(changedAt, comment.updatedDate());
        }

        @Test
        @DisplayName("comment needs basic fields")
        void rejectsMissingFields() {
                assertThrows(IllegalArgumentException.class,
                                () -> Comment.create("", "content", writer, CommentPurpose.GENERAL, createdAt));
                assertThrows(IllegalArgumentException.class,
                                () -> Comment.create("C-1", "", writer, CommentPurpose.GENERAL, createdAt));
                assertThrows(NullPointerException.class,
                                () -> Comment.create("C-1", "content", null, CommentPurpose.GENERAL, createdAt));
                assertThrows(NullPointerException.class,
                                () -> Comment.create("C-1", "content", writer, CommentPurpose.GENERAL, null));
                assertThrows(IllegalArgumentException.class,
                                () -> Comment.newForIssue(0L, "content", writer, CommentPurpose.GENERAL, createdAt));
        }

        @Test
        @DisplayName("loaded comment needs valid fields")
        void rejectsInvalidLoadedComment() {
                assertThrows(IllegalArgumentException.class,
                                () -> Comment.fromPersistence(1L, 100L, "", "content", CommentPurpose.GENERAL,
                                                createdAt, createdAt));
                assertThrows(IllegalArgumentException.class,
                                () -> Comment.fromPersistence(1L, 100L, "dev1", " ", CommentPurpose.GENERAL,
                                                createdAt, createdAt));
                assertThrows(NullPointerException.class,
                                () -> Comment.fromPersistence(1L, 100L, "dev1", "content", CommentPurpose.GENERAL,
                                                null, createdAt));
                assertThrows(NullPointerException.class,
                                () -> Comment.fromPersistence(1L, 100L, "dev1", "content", CommentPurpose.GENERAL,
                                                createdAt, null));
        }

        @Test
        @DisplayName("comment edit still needs text and time")
        void rejectsInvalidEdit() {
                var comment = Comment.create("C-1", "content", writer, CommentPurpose.GENERAL, createdAt);

                assertThrows(IllegalArgumentException.class,
                                () -> comment.changeContent(" ", createdAt.plusMinutes(1)));
                assertThrows(NullPointerException.class, () -> comment.changeContent("updated", null));
        }
}
