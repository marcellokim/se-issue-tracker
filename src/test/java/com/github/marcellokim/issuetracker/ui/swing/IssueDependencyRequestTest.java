package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue dependency request")
class IssueDependencyRequestTest {

    @Test
    @DisplayName("validates add dependency issue ids")
    void validatesAddDependencyRequest() {
        IssueDependencyRequest request = IssueDependencyRequest.add(3L, 7L);

        assertEquals(IssueDependencyMode.ADD, request.mode());
        assertEquals(3L, request.blockingIssueId());
        assertEquals(7L, request.blockedIssueId());

        assertThrows(IllegalArgumentException.class, () -> IssueDependencyRequest.add(0L, 7L));
        assertThrows(IllegalArgumentException.class, () -> IssueDependencyRequest.add(3L, 0L));
    }

    @Test
    @DisplayName("validates remove dependency issue ids")
    void validatesRemoveDependencyRequest() {
        IssueDependencyRequest request = IssueDependencyRequest.remove(3L, 7L);

        assertEquals(IssueDependencyMode.REMOVE, request.mode());
        assertEquals(3L, request.blockingIssueId());
        assertEquals(7L, request.blockedIssueId());

        assertThrows(IllegalArgumentException.class, () -> IssueDependencyRequest.remove(-1L, 7L));
        assertThrows(IllegalArgumentException.class, () -> IssueDependencyRequest.remove(3L, -1L));
    }
}
