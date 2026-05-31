package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.Priority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue edit context")
class IssueEditContextTest {

    @Test
    @DisplayName("keeps existing issue text as loaded")
    void keepsExistingIssueText() {
        IssueEditContext context = new IssueEditContext(" Login bug ", "", Priority.MAJOR);

        assertEquals(" Login bug ", context.title());
        assertEquals("", context.description());
        assertEquals(Priority.MAJOR, context.priority());
    }

    @Test
    @DisplayName("requires loaded fields to be present")
    void requiresLoadedFields() {
        assertThrows(NullPointerException.class, () -> new IssueEditContext(null, "", Priority.MAJOR));
        assertThrows(NullPointerException.class, () -> new IssueEditContext("title", null, Priority.MAJOR));
        assertThrows(NullPointerException.class, () -> new IssueEditContext("title", "", null));
    }
}
