package com.github.marcellokim.issuetracker.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class KNNAssignmentRecommendation {

    public final List<String> calculateRecomendation(String targetTitle, String targetDescription, List<UserRecord> listToCalculateRecommendation){
        Objects.requireNonNull(targetTitle, "targetTitle");
        List<String> titleKeys = List.of(targetTitle.replaceAll("[^a-zA-Z0-9가-힣]", " ").split("\\s+"));
        Set<String> targetKeyWords = compareKeywords(titleKeys);

        int corpusSize = listToCalculateRecommendation.size();
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (UserRecord record : listToCalculateRecommendation){
            Set<String> uniqueTokens = new HashSet<>(tokenize(record.description()));
            for (String token : uniqueTokens){ documentFrequency.merge(token, 1, (a, b) -> a + b); }
        }
        Map<String, Double> idfMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()){
            idfMap.put(entry.getKey(), Math.log((double) corpusSize / entry.getValue()));
        }
        double tfidfWeight = Math.min((Math.exp(Math.E * corpusSize / 1000.0) - 1) / (Math.exp(Math.E) - 1), 1.0) * 0.6;

        List<SimlilarityResult> CompareResult = new ArrayList<>();
        for (UserRecord record : listToCalculateRecommendation){
            List<String> recordKeys = List.of(record.title().replaceAll("[^a-zA-Z0-9가-힣]", " ").split("\\s+"));
            Set<String> recordKeyWords = compareKeywords(recordKeys);
            double jaccard = jaccardCompare(targetKeyWords, recordKeyWords);
            double cosine = TfIdfCompare(targetDescription, record.description(), idfMap);
            double combinedScore = jaccard * (1 - tfidfWeight) + cosine * tfidfWeight;
            CompareResult.add(new SimlilarityResult(combinedScore, record.userId()));
        }
        
        CompareResult.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));
        Set<String> uniqueCompareResult = new LinkedHashSet<>();
        for(SimlilarityResult result : CompareResult){ uniqueCompareResult.add(result.userId()); }
        if (uniqueCompareResult.size() < 3){
            List<String> fallback = fallbackRecommendation(listToCalculateRecommendation, uniqueCompareResult);
            for (String user : fallback){
                if (uniqueCompareResult.size() >= 3){ break; }
                uniqueCompareResult.add(user);
            }
        }
        return uniqueCompareResult.stream().toList();
    }

    public record UserRecord(String title, String description, String userId){}
    private record SimlilarityResult(double similarity, String userId){}

    private Set<String> compareKeywords(List<String> titleKeys){
        Objects.requireNonNull(titleKeys, "titleKeys");
        Set<String> matchedKeywords = new HashSet<>();
        for (String searchKey : titleKeys){
            if (KEYWORDS.contains(searchKey)){ matchedKeywords.add(searchKey); }
        }
        return matchedKeywords;
    }

    private double jaccardCompare(Set<String> keySet, Set<String> setToCompare){
        Objects.requireNonNull(keySet, "keySet");
        Objects.requireNonNull(setToCompare, "setToCompare");
        Set<String> intersection = new HashSet<>(keySet);
        Set<String> union = new HashSet<>(keySet);
        intersection.retainAll(setToCompare);
        union.addAll(setToCompare);
        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }
    private List<String> fallbackRecommendation(List<UserRecord> allUser, Set<String> allreadySelectedUser){
        Map<String, Integer> fallBackUsers = new HashMap<>();
        for(UserRecord user : allUser){
            if (allreadySelectedUser.contains(user.userId()) == false){ fallBackUsers.merge(user.userId(), 1, (sum, newCount) -> sum + newCount); }
        }
        return fallBackUsers.entrySet().stream().sorted((a, b) -> Integer.compare(a.getValue(), b.getValue())).map(e -> e.getKey()).toList();
    }
    
    private double TfIdfCompare(String targetDescription, String recordDescription, Map<String, Double> idfMap){
        Objects.requireNonNull(targetDescription, "targetDescription");
        Objects.requireNonNull(recordDescription, "recordDescription");
        List<String> targetTokens = Objects.requireNonNull(tokenize(targetDescription), "tokenize(targetDescription)");
        List<String> recordTokens = Objects.requireNonNull(tokenize(recordDescription), "tokenize(recordDescription)");

        Map<String, Double> targetTfIdf = new HashMap<>();
        for (String token : targetTokens){ targetTfIdf.merge(token, 1.0 / targetTokens.size(), (a, b) -> a + b); }
        Map<String, Double> recordTfIdf = new HashMap<>();
        for (String token : recordTokens){ recordTfIdf.merge(token, 1.0 / recordTokens.size(), (a, b) -> a + b); }
        for (String token : targetTfIdf.keySet()){ targetTfIdf.put(token, targetTfIdf.get(token) * idfMap.getOrDefault(token, 0.0)); }
        for (String token : recordTfIdf.keySet()){ recordTfIdf.put(token, recordTfIdf.get(token) * idfMap.getOrDefault(token, 0.0)); }

        Set<String> allTerms = new HashSet<>(targetTfIdf.keySet());
        allTerms.addAll(recordTfIdf.keySet());
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (String term : allTerms){
            double a = targetTfIdf.getOrDefault(term, 0.0);
            double b = recordTfIdf.getOrDefault(term, 0.0);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    private List<String> tokenize(String text){
        if (text == null || text.isBlank()) return List.of();
        List<String> tokens = new ArrayList<>();
        for (String word : text.toLowerCase().replaceAll("[^a-zA-Z0-9가-힣]", " ").split("\\s+")){
            if (word.isBlank() == false && STOP_WORDS.contains(word) == false){ tokens.add(word); }
        }
        return tokens;
    }

    private static final Set<String> KEYWORDS = new HashSet<>(List.of(
    // 버그/오류
    "error", "bug", "exception", "null", "crash", "fail", "failure",
    "overflow", "leak", "deadlock", "race", "corrupt", "infinite",
    "duplicate", "missing", "broken", "invalid", "unexpected",
    "inconsistent", "undefined", "unhandled", "timeout", "hang",
    "freeze", "flicker",

    // UI
    "button", "page", "screen", "form", "input", "field", "modal",
    "dialog", "popup", "dropdown", "menu", "tab", "table", "list",
    "card", "panel", "header", "footer", "sidebar", "navbar",
    "checkbox", "radio", "slider", "tooltip", "icon", "frontend",

    // 사용자 동작들
    "login", "logout", "signup", "register", "submit", "click",
    "scroll", "drag", "drop", "upload", "download", "search",
    "filter", "sort", "select", "delete", "edit", "update",
    "create", "save", "load", "refresh", "redirect", "navigate",
    "export",

    // 시스템/인프라 관련
    "api", "server", "client", "database", "cache", "session",
    "token", "socket", "connection", "request", "response", "query",
    "migration", "deploy", "config", "environment", "docker",
    "memory", "cpu", "disk", "network", "proxy", "ssl", "dns",
    "port", "backend", 

    // 기능
    "auth", "authentication", "authorization", "permission", "role",
    "password", "email", "notification", "payment", "cart", "order",
    "profile", "setting", "dashboard", "report", "chart", "log",
    "audit", "import", "sync", "batch", "schedule", "queue",
    "webhook", "callback",

    // 데이터 상태
    "empty", "blank", "default", "stale",
    "expired", "locked", "pending", "active", "inactive", "deleted",
    "archived", "draft", "published", "approved", "rejected",
    "assigned", "resolved", "closed", "open", "blocked", "overdue",
    "priority", "status",

    // 성능/품질
    "slow", "performance", "latency", "throughput",
    "scalability", "optimization", "bottleneck", "delay", "lag",
    "responsive", "render", "compile", "build", "test", "coverage",
    "regression", "compatibility", "accessibility", "security",

    // 데이터 처리 문제
    "parse", "format", "encode", "decode", "serialize", "validate",
    "sanitize", "truncate", "convert", "transform", "mapping",
    "binding", "injection", "escape", "regex",

    // 한국어 키워드
    "오류", "버그", "에러", "실패", "충돌", "느림", "깨짐",
    "안됨", "누락", "중복", "권한", "로딩", "화면", "버튼", "페이지",
    "예외", "널", "크래시",
    "오버플로우", "누수", "데드락", "손상", "무한",
    "잘못된", "불일치", "미정의", "미처리", "타임아웃",
    "멈춤", "프리즈", "깜빡임",
    "폼", "양식", "입력", "필드", "모달",
    "대화상자", "팝업", "드롭다운", "메뉴", "탭", "테이블", "목록",
    "카드", "패널", "헤더", "푸터", "사이드바", "체크박스", "아이콘",
    "로그인", "로그아웃", "가입", "등록", "제출", "클릭", "스크롤",
    "드래그", "업로드", "다운로드", "검색", "필터", "정렬", "선택",
    "삭제", "편집", "수정", "생성", "저장", "새로고침", "이동", "내보내기",
    "서버", "클라이언트", "데이터베이스", "캐시", "세션", "토큰",
    "소켓", "연결", "요청", "응답", "쿼리", "배포", "설정", "환경", "메모리", "디스크",
    "네트워크", "프록시", "포트", "인증", "인가", "역할", "비밀번호", "이메일", "알림",
    "결제", "장바구니", "주문", "프로필", "대시보드", "리포트",
    "차트", "로그", "감사", "동기화", "배치", "큐",
    "빈값", "공백", "기본값", "만료", "잠김", "대기", "활성",
    "비활성", "삭제됨", "승인", "반려", "거부", "배정됨", "해결됨", "종료됨", "차단됨", "우선순위", "상태",
    "성능", "지연", "부하", "최적화", "병목", "렉",
    "렌더링", "컴파일", "빌드", "테스트", "회귀", "호환성", "보안",
    "파싱", "포맷", "인코딩", "디코딩", "직렬화", "검증", "변환",
    "매핑", "바인딩", "주입", "정규식", "프론트엔드", "백엔드"));

    private static final Set<String> STOP_WORDS = Set.of(
    "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
    "have", "has", "had", "do", "does", "did", "will", "would", "shall",
    "should", "may", "might", "must", "can", "could",
    "i", "you", "he", "she", "it", "we", "they", "me", "him", "her",
    "us", "them", "my", "your", "his", "its", "our", "their",
    "this", "that", "these", "those",
    "in", "on", "at", "to", "for", "of", "with", "by", "from", "as",
    "into", "through", "during", "before", "after", "above", "below",
    "between", "under", "about", "against", "over",
    "and", "but", "or", "nor", "not", "so", "yet", "both", "either",
    "neither", "each", "every", "all", "any", "few", "more", "most",
    "some", "no", "only", "same", "than", "too", "very",
    "if", "then", "else", "when", "where", "why", "how", "what", "which",
    "who", "whom", "whose",
    "just", "also", "already", "still", "even", "now", "here", "there",
    "again", "once", "further", "such", "much", "many",
    "은", "는", "이", "가", "을", "를", "에", "에서", "으로", "로",
    "와", "과", "의", "도", "만", "까지", "부터", "에게", "한테",
    "고", "며", "면", "지만", "그리고", "그런데", "하지만", "또는",
    "그래서", "따라서", "때문에", "위해", "대해", "통해",
    "것", "수", "등", "때", "중", "후", "전", "위", "아래",
    "있다", "없다", "하다", "되다", "않다", "있는", "없는", "하는", "되는",
    "했다", "됐다", "있습니다", "합니다", "입니다", "이고요", "인데요",
    "입니다만", "이지만");

}
