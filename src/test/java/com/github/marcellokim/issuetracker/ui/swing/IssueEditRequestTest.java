package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.Priority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue edit request")
class IssueEditRequestTest {

    @Test
    @DisplayName("validates title and description for issue update")
    void validatesIssueUpdate() {
        IssueEditRequest request = IssueEditRequest.update(" Login bug ", " Updated description ");

        assertEquals(IssueEditMode.UPDATE, request.mode());
        assertEquals("Login bug", request.title());
        assertEquals("Updated description", request.description());
        assertNull(request.priority());
        assertNull(request.comment());

        assertThrows(IllegalArgumentException.class, () -> IssueEditRequest.update(" ", "description"));
        assertThrows(IllegalArgumentException.class, () -> IssueEditRequest.update("title", " "));
    }

    @Test
    @DisplayName("validates priority change")
    void validatesPriorityChange() {
        IssueEditRequest request = IssueEditRequest.changePriority(Priority.MINOR);

        assertEquals(IssueEditMode.CHANGE_PRIORITY, request.mode());
        assertEquals(Priority.MINOR, request.priority());

        assertThrows(NullPointerException.class, () -> IssueEditRequest.changePriority(null));
    }

    @Test
    @DisplayName("validates soft delete comment")
    void validatesSoftDeleteComment() {
        IssueEditRequest request = IssueEditRequest.softDelete(" remove duplicate ");

        assertEquals(IssueEditMode.SOFT_DELETE, request.mode());
        assertEquals("remove duplicate", request.comment());

        assertThrows(IllegalArgumentException.class, () -> IssueEditRequest.softDelete(" "));
    }
}
