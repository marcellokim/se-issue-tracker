package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.service.KNNAssignmentRecommendation.UserRecord;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KNN assignment recommendation")
class KNNAssignmentRecommendationTest {

    private final KNNAssignmentRecommendation knn = new KNNAssignmentRecommendation();

    @Test
    @DisplayName("closest title match comes first")
    void closestTitleComesFirst() {
        List<UserRecord> records = List.of(
                new UserRecord("button click scroll", "", "dev2"),
                new UserRecord("login fail error", "", "dev1"),
                new UserRecord("login page error crash", "", "dev3"));

        List<String> result = knn.calculateRecomendation("login error page", "", records);

        assertEquals("dev3", result.get(0));
        assertEquals("dev1", result.get(1));
    }

    @Test
    @DisplayName("same user is listed once")
    void sameUserListedOnce() {
        List<UserRecord> records = List.of(
                new UserRecord("login error", "", "dev1"),
                new UserRecord("login fail", "", "dev1"),
                new UserRecord("login crash", "", "dev1"));

        List<String> result = knn.calculateRecomendation("login error", "", records);

        assertEquals(1, result.size());
        assertEquals("dev1", result.get(0));
    }

    @Test
    @DisplayName("no history means no recommendation")
    void noHistoryMeansNoRecommendation() {
        List<String> result = knn.calculateRecomendation("login error", "", List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("unknown words fall back to candidates")
    void unknownWordsUseFallback() {
        List<UserRecord> records = List.of(
                new UserRecord("login error", "", "dev1"),
                new UserRecord("button click", "", "dev2"));

        List<String> result = knn.calculateRecomendation("xyzabc foobar", "", records);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("comma title is split")
    void commaTitleIsSplit() {
        List<UserRecord> records = List.of(
                new UserRecord("login,error,page", "", "dev1"),
                new UserRecord("button,click", "", "dev2"));

        List<String> result = knn.calculateRecomendation("login,error", "", records);

        assertEquals("dev1", result.get(0));
    }

    @Test
    @DisplayName("description helps the order")
    void descriptionHelpsOrder() {
        List<UserRecord> records = List.of(
                new UserRecord("error", "database connection timeout query failed", "dev1"),
                new UserRecord("error", "button style color font changed", "dev2"),
                new UserRecord("error", "database query connection pool exhausted", "dev3"));

        List<String> result = knn.calculateRecomendation("error", "database connection query timeout", records);

        assertTrue(result.contains("dev1"));
        assertTrue(result.contains("dev3"));
    }

    @Test
    @DisplayName("special characters are ignored")
    void specialCharactersAreIgnored() {
        List<UserRecord> records = List.of(
                new UserRecord("login@error#page!", "", "dev1"),
                new UserRecord("button$click%test", "", "dev2"));

        List<String> result = knn.calculateRecomendation("login#error", "", records);

        assertEquals("dev1", result.get(0));
    }

    @Test
    @DisplayName("common words do not dominate")
    void commonWordsDoNotDominate() {
        List<UserRecord> records = List.of(
                new UserRecord("error", "the server is not responding to the request", "dev1"),
                new UserRecord("error", "server responding request", "dev2"));

        List<String> result = knn.calculateRecomendation("error", "server request responding", records);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("fallback fills the list")
    void fallbackFillsList() {
        List<UserRecord> records = List.of(
                new UserRecord("login error", "", "dev1"),
                new UserRecord("button click", "", "dev2"),
                new UserRecord("scroll page", "", "dev3"),
                new UserRecord("menu tab", "", "dev4"));

        List<String> result = knn.calculateRecomendation("xyzabc", "", records);

        assertTrue(result.size() >= 3);
    }

    @Test
    @DisplayName("empty description is fine")
    void emptyDescriptionIsFine() {
        List<UserRecord> records = List.of(
                new UserRecord("login error", "", "dev1"));

        List<String> result = knn.calculateRecomendation("login error", "", records);

        assertEquals(1, result.size());
        assertEquals("dev1", result.get(0));
    }

    @Test
    @DisplayName("title is required")
    void titleIsRequired() {
        assertThrows(NullPointerException.class,
                () -> knn.calculateRecomendation(null, "", List.of()));
    }
}
