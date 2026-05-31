package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue status change request")
class IssueStatusChangeRequestTest {

    @Test
    @DisplayName("trims status change comment")
    void trimsStatusChangeComment() {
        IssueStatusChangeRequest request = new IssueStatusChangeRequest(IssueStatus.FIXED, " fixed in swing ");

        assertEquals(IssueStatus.FIXED, request.targetStatus());
        assertEquals("fixed in swing", request.comment());
    }

    @Test
    @DisplayName("rejects blank status change comment")
    void rejectsBlankStatusChangeComment() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueStatusChangeRequest(IssueStatus.FIXED, " "));
    }

    @Test
    @DisplayName("maps detail action names to target status")
    void mapsDetailActionNamesToTargetStatus() {
        assertEquals(Optional.of(IssueStatus.FIXED), IssueStatusChangeActions.targetStatus("MARK_FIXED"));
        assertEquals(Optional.of(IssueStatus.RESOLVED), IssueStatusChangeActions.targetStatus("RESOLVE"));
        assertEquals(Optional.of(IssueStatus.ASSIGNED), IssueStatusChangeActions.targetStatus("REJECT_FIX"));
        assertEquals(Optional.of(IssueStatus.CLOSED), IssueStatusChangeActions.targetStatus("CLOSE"));
        assertEquals(Optional.of(IssueStatus.REOPENED), IssueStatusChangeActions.targetStatus("REOPEN"));
        assertEquals(Optional.empty(), IssueStatusChangeActions.targetStatus("ADD_COMMENT"));
    }

    @Test
    @DisplayName("maps detail action names to readable labels")
    void mapsDetailActionNamesToReadableLabels() {
        assertEquals("Mark fixed", IssueStatusChangeActions.label("MARK_FIXED"));
        assertEquals("Resolve", IssueStatusChangeActions.label("RESOLVE"));
        assertEquals("Reject fix", IssueStatusChangeActions.label("REJECT_FIX"));
        assertEquals("ADD_COMMENT", IssueStatusChangeActions.label("ADD_COMMENT"));
    }
}
