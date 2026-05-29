package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
