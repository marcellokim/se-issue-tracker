package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue assignment request")
class IssueAssignmentRequestTest {

    @Test
    @DisplayName("maps assignment detail actions to modes")
    void mapsAssignmentDetailActionsToModes() {
        assertEquals(Optional.of(IssueAssignmentMode.ASSIGN), IssueAssignmentActions.mode("ASSIGN"));
        assertEquals(Optional.of(IssueAssignmentMode.REASSIGN_DEV), IssueAssignmentActions.mode("REASSIGN_DEV"));
        assertEquals(Optional.of(IssueAssignmentMode.CHANGE_TESTER), IssueAssignmentActions.mode("CHANGE_TESTER"));
        assertEquals(Optional.empty(), IssueAssignmentActions.mode("ADD_COMMENT"));
    }

    @Test
    @DisplayName("delegates start assignment to the available concrete assignment action")
    void delegatesStartAssignmentToAvailableConcreteAction() {
        assertEquals(
                "ASSIGN",
                IssueAssignmentActions.effectiveAction("START_ASSIGNMENT", Set.of("START_ASSIGNMENT", "ASSIGN")));
        assertEquals(
                "REASSIGN_DEV",
                IssueAssignmentActions.effectiveAction(
                        "START_ASSIGNMENT",
                        Set.of("START_ASSIGNMENT", "REASSIGN_DEV")));
        assertEquals(
                "CHANGE_TESTER",
                IssueAssignmentActions.effectiveAction(
                        "START_ASSIGNMENT",
                        Set.of("START_ASSIGNMENT", "CHANGE_TESTER")));
        assertEquals("START_ASSIGNMENT", IssueAssignmentActions.effectiveAction("START_ASSIGNMENT", Set.of()));
    }

    @Test
    @DisplayName("validates assignment mode required fields")
    void validatesAssignmentModeRequiredFields() {
        IssueAssignmentRequest assign = new IssueAssignmentRequest(
                IssueAssignmentMode.ASSIGN,
                " dev1 ",
                " tester1 ");
        assertEquals("dev1", assign.assigneeId());
        assertEquals("tester1", assign.verifierId());

        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueAssignmentRequest(IssueAssignmentMode.ASSIGN, "dev1", " "));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueAssignmentRequest(IssueAssignmentMode.REASSIGN_DEV, " ", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueAssignmentRequest(IssueAssignmentMode.CHANGE_TESTER, null, " "));
    }
}
