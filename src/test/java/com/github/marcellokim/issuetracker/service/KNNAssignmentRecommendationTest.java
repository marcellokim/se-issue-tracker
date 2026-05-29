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
    @DisplayName("returns user with highest Jaccard similarity first")
    void returnsHighestSimilarityFirst() {
        List<UserRecord> records = List.of(
                new UserRecord("button click scroll", "", "dev2"),
                new UserRecord("login fail error", "", "dev1"),
                new UserRecord("login page error crash", "", "dev3"));

        List<String> result = knn.calculateRecomendation("login error page", "", records);

        assertEquals("dev3", result.get(0));
        assertEquals("dev1", result.get(1));
    }

    @Test
    @DisplayName("deduplicates same user from multiple records")
    void deduplicatesSameUser() {
        List<UserRecord> records = List.of(
                new UserRecord("login error", "", "dev1"),
                new UserRecord("login fail", "", "dev1"),
                new UserRecord("login crash", "", "dev1"));

        List<String> result = knn.calculateRecomendation("login error", "", records);

        assertEquals(1, result.size());
        assertEquals("dev1", result.get(0));
    }

    @Test
    @DisplayName("returns empty list when no records provided")
    void returnsEmptyForNoRecords() {
        List<String> result = knn.calculateRecomendation("login error", "", List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("handles target title with no matching keywords")
    void handlesNoMatchingKeywords() {
        List<UserRecord> records = List.of(
                new UserRecord("login error", "", "dev1"),
                new UserRecord("button click", "", "dev2"));

        List<String> result = knn.calculateRecomendation("xyzabc foobar", "", records);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("handles both target and record having no matching keywords")
    void handlesBothNoKeywords() {
        List<UserRecord> records = List.of(
                new UserRecord("xyzabc", "", "dev1"));

        List<String> result = knn.calculateRecomendation("foobar", "", records);

        assertEquals(1, result.size());
        assertEquals("dev1", result.get(0));
    }

    @Test
    @DisplayName("Korean keywords are matched correctly")
    void koreanKeywordsMatched() {
        List<UserRecord> records = List.of(
                new UserRecord("로그인 오류 화면", "", "dev1"),
                new UserRecord("버튼 클릭 스크롤", "", "dev2"),
                new UserRecord("로그인 에러 페이지", "", "dev3"));

        List<String> result = knn.calculateRecomendation("로그인 오류 페이지", "", records);

        assertEquals("dev1", result.get(0));
    }

    @Test
    @DisplayName("comma-separated titles are split into keywords")
    void commaSeparatedTitlesAreSplit() {
        List<UserRecord> records = List.of(
                new UserRecord("login,error,page", "", "dev1"),
                new UserRecord("button,click", "", "dev2"));

        List<String> result = knn.calculateRecomendation("login,error", "", records);

        assertEquals("dev1", result.get(0));
    }

    @Test
    @DisplayName("TF-IDF description similarity affects ranking")
    void tfidfDescriptionAffectsRanking() {
        List<UserRecord> records = List.of(
                new UserRecord("error", "database connection timeout query failed", "dev1"),
                new UserRecord("error", "button style color font changed", "dev2"),
                new UserRecord("error", "database query connection pool exhausted", "dev3"));

        List<String> result = knn.calculateRecomendation("error", "database connection query timeout", records);

        assertTrue(result.contains("dev1"));
        assertTrue(result.contains("dev3"));
    }

    @Test
    @DisplayName("special characters in title are normalized")
    void specialCharactersNormalized() {
        List<UserRecord> records = List.of(
                new UserRecord("login@error#page!", "", "dev1"),
                new UserRecord("button$click%test", "", "dev2"));

        List<String> result = knn.calculateRecomendation("login#error", "", records);

        assertEquals("dev1", result.get(0));
    }

    @Test
    @DisplayName("stop words are filtered from description tokens")
    void stopWordsFiltered() {
        List<UserRecord> records = List.of(
                new UserRecord("error", "the server is not responding to the request", "dev1"),
                new UserRecord("error", "server responding request", "dev2"));

        List<String> result = knn.calculateRecomendation("error", "server request responding", records);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("fallback fills candidates when fewer than 3 unique users")
    void fallbackFillsCandidates() {
        List<UserRecord> records = List.of(
                new UserRecord("login error", "", "dev1"),
                new UserRecord("button click", "", "dev2"),
                new UserRecord("scroll page", "", "dev3"),
                new UserRecord("menu tab", "", "dev4"));

        List<String> result = knn.calculateRecomendation("xyzabc", "", records);

        assertTrue(result.size() >= 3);
    }

    @Test
    @DisplayName("empty description returns zero TF-IDF score without error")
    void emptyDescriptionReturnsZeroTfidf() {
        List<UserRecord> records = List.of(
                new UserRecord("login error", "", "dev1"));

        List<String> result = knn.calculateRecomendation("login error", "", records);

        assertEquals(1, result.size());
        assertEquals("dev1", result.get(0));
    }

    @Test
    @DisplayName("null title throws NullPointerException")
    void nullTitleThrows() {
        assertThrows(NullPointerException.class,
                () -> knn.calculateRecomendation(null, "", List.of()));
    }
}
