# 15. 통계

## 진입 조건

- Actor: PL | Dev | Tester (Admin 제외)
- 선행 화면: 이슈 목록 → 통계 보기 버튼
- 전달 값: `projectId`

## 화면 동작 API

### 1. 통계 조회

- **호출 시점**: 화면 진입 시 (전체 기간)
- **메서드**: `StatisticsController.viewStatistics(projectId)`
- **파라미터**: projectId (long)
- **확장 메서드**: `StatisticsController.viewStatistics(projectId, dailyFromInclusive, dailyToInclusive, monthlyFromInclusive, monthlyToInclusive)`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| projectId | long | Y | 프로젝트 ID |
| dailyFromInclusive | LocalDate | N | 일별 통계 시작일 |
| dailyToInclusive | LocalDate | N | 일별 통계 종료일 |
| monthlyFromInclusive | YearMonth | N | 월별 통계 시작월 |
| monthlyToInclusive | YearMonth | N | 월별 통계 종료월 |

> 간단 버전(`projectId`만)은 전체 기간 조회. 날짜 범위 필터가 필요하면 확장 버전 사용.
- **반환**: `StatisticsReportResult`

| 필드 | 타입 | 설명 |
|---|---|---|
| statusCounts | Map\<IssueStatus, Integer\> | 상태별 이슈 수 |
| priorityCounts | Map\<Priority, Integer\> | 우선순위별 이슈 수 |
| dailyCounts | List\<DailyCountResult\> | 일별 이슈 등록 수 |
| monthlyCounts | List\<MonthlyCountResult\> | 월별 이슈 등록 수 |
| monthlyStatusCounts | Map\<YearMonth, Map\<IssueStatus, Integer\>\> | 월별 상태별 분포 |
| monthlyPriorityCounts | Map\<YearMonth, Map\<Priority, Integer\>\> | 월별 우선순위별 분포 |
| dailyStatusChangeCounts | List\<DailyCountResult\> | 일별 상태 변경 수 |
| monthlyStatusChangeCounts | List\<MonthlyCountResult\> | 월별 상태 변경 수 |
| dailyCommentCounts | List\<DailyCountResult\> | 일별 코멘트 수 |
| monthlyCommentCounts | List\<MonthlyCountResult\> | 월별 코멘트 수 |

- `DailyCountResult`:

| 필드 | 타입 | 설명 |
|---|---|---|
| date | LocalDate | 날짜 |
| count | int | 건수 |

- `MonthlyCountResult`:

| 필드 | 타입 | 설명 |
|---|---|---|
| month | YearMonth | 월 |
| count | int | 건수 |

## 화면 전이

| 동작 | 다음 화면 |
|---|---|
| 뒤로가기 | 이슈 목록 |
