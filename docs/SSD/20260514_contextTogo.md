##서론
20260513에 SSD 등 심화적인 수정사항이 들어왔다. 해당 수정사항들은 SSD_확정 과 SSD_윤동을 다음 과정으로 검수, 통합하는 과정에서 발생했다:
1. 윤동이 추가한 SSD 검수 - 컨텍스트를 기반
2. 기존에 확정됐던 SSD 검수(conflict) - 컨텍스트/확정SSD/윤동SSD와 비교
3. 어제 회의에서 메모한 SSD들 추가 검수(확정 SSD를 메인으로)

두 SSD와 컨텍스트(회의 기록 등)를 기반으로 검수, 통합하려고 했지만, 직접 검수하는 과정에서 추가 수정사항이 발생했다:
[만약 assignee 필드랑 verifier 필드를 드랍 안하면 만약 dev랑 tester가 '나에게 assign된 이슈'나 '내가 verifier로 설정된 이슈'라고 '이슈 상세 검색'했을 때 resolved/closed/deleted 이슈에 assignee, verifier 필드가 남아있으면 이게 검색 스코프에 잡혀서 시스템적으로 resolved/closed/deleted 이슈를 검색 스코프에서 제거하는 과정이 필요한데 이게 프로젝트 일관성이랑 안맞음],[reopen한 직후에 assignee랑 verifier 지정할라고 할때 만약 assignee랑 verifier 필드가 이전값 그대로 있으면 reopened인지 아니면 첫 resolved 상태인지 모호해서 resolved로 되면 verifier랑 assignee 필드값 드랍하는걸로 한다고] 등의 이유로, resolver 필드를 추가하고, fixer와 resolver을 다루는 방식을 조율했다. 또한, 정식 파이프라인 중간에서 'PL이 dev나 tester을 교체하고 싶을 때'를 대비해, UC5 - RA와 UC5 - RV를 추가했다.
**중요!** 해당 사항들 및 변경사항들을 모두 담은 핵심 문서는 다음과 같다: Changes.docx

위 사항들에 대해 판단한 codex-review-defense-vs-meeting-20260513.pdf 문서를 기반으로 크게 4가지 문제점을 추출했다:
1. UC5 includes UC8 관계가 깨졌다
2. ASSIGNMENT_CHANGED enum이 도메인 모델에 없다
3. UC10 operation의 role 파라미터 표현 문제
4. resolver 도입 후 동기화 부족

이 중, 2,3,4번은 다음과 같이 해결하기로 결정:
2번은 도메인모델에 추가하면 되고, 3번은 '사용자가 인증 role을 임의로 넘긴다'가 아니라, '검색할 role의 스코프를 지정한다'임. 의미가 이렇게 바뀐다면 문제없는지 재검사 필요. 4번resolver 도입에 따라 전면 수정 필요함. 이거 수정 필요지점들 조사 필요함. uc 명세부터 바꿔야됨. 도메인 모델 note, OC는 당연.

1번 문제는 합의가 필요해서, 회의를 진행했다. 다음은 이번 회의 시 고려사항들이다:

##회의사항(나열)
 - 위 사항들에 대해 회의를 진행했다. 다음은 그 내용들을 나열식으로 제시하겠다.

근데 fixer는 넣어야하는게 과제 가이드라인에
최소 필드 요소라고 나와있긴해서
무조건 넣긴 해야함

resolved -> fixed는 없으니까 [fixed -> resolved에서 verifier 드랍, resolver에 추가] 이건 문제없고 
fixed -> assigned는 있어서 [assigned -> fixed에서 fixer에 dev 추가]는 맞는데
assignee 드랍 포인트로는 적절하진 않은듯

 ㄴ> 요약하자면, assignee와 verifier 필드 드랍 시점 조정 필요

RASP Information Expert: 누가 fixed로 바꿨는지는 Issue보다 IssueHistory가 더 잘 안다.
SRP: Issue는 현재 상태와 핵심 속성, IssueHistory는 변경 사건을 책임진다.
MVC: UI는 “Fixer” 필드를 보여줄 수 있지만, 그 값은 domain/query layer가 history에서 계산해 제공하면 된다.
중복 방지: Issue.fixer와 IssueHistory.changedBy가 서로 안 맞는 문제가 사라진다.
resolver도 이하동문
fixer = latest IssueHistory where
  action = STATUS_CHANGED
  previousValue = ASSIGNED
  newValue = FIXED
  changedBy = Dev
근데 이게 과제에서 issue의 최소 필드가 fixer라는게 좀 그르네

그럼 fixer를 issuehistory에 넣어야지
issue에 넣으면 안되지

솔직히 둘 다 넣어도 됨
issue나 issueHistory나 어찌됐든 '이슈가 수정 완료되었고, assignee와 verifier는 더 이상 해당 이슈에 대해 검색 스코프 및 할당 스코프에 잡히지 않는다'만 성립하면 되니까
어디에 넣냐까지는 생각 안하긴 했음 ㅋㅋ
issue가 수정 완료되었다는건 status로 알 수 있으니까
history에 넣는게 더 맞긴 하겠다

 ㄴ> 요약하자면, fixer와 resolver 필드는 어디에 들어가야 하는가?

어제 reopen -> assigned로 가는 과정을 UC5 이슈 배정이 아니라 UC6 이슈 상태 변경으로 봤는데(UC8 Assignee 추천의 include 관계를 회피하기 위해서)
UC5 - RA RV 들어오면서 그냥 회피할 수가 없어짐
ㄴ> 윤동이 보고서에 문제제시했듯이 UC5로 봐야되는데 그러면 UC8이 include라 문제가 됨
그냥 UC5 <- UC8 extend로 가는건 어떤가요 - [근거: 이미 include는 2개(큰 관점에서) 있음]

이렇게 되면 [1. reopen -> assigned / 2. UC5 - RA(PL이 개발자 교체) / 3. UC5 - RV(PL이 tester 교체)] 셋 모두 UC5로 넣을수있음 

기존꺼는 UC5에서 어떤 시나리오든지 필수적으로 다 참가해야하긴해서 include 관계로 볼 수 있긴한데
지금꺼는 다 세분화되서 한 시나리오에서는 안쓰고, 다른 시나리오에서 안쓰이기도 하고

근데 독립적인 class department로 구현해야되는 기능이긴 한데
흠
아니면 그냥 [1. reopen -> assigned / 2. UC5 - RA(PL이 개발자 교체) / 3. UC5 - RV(PL이 tester 교체)] 얘네 셋 다 UC5로 넣어버리고
UC5 <- UC8 extend로 바꾼다음에 그냥 버튼식으로 만든다고 가정하면 되지 않음?
그러면 UC5에서 4가지 경우가 나오는건데:
0) 기본적인 경우(new -> assigned)
1) reopen -> assigned
2) UC5 - RA(PL이 개발자 교체)
3) UC5 - RV(PL이 tester 교체)
여기서 전부 "버튼식"으로 PL이 원하면 눌러서 추천받게 할 수 있지않음?

UCD에서 UC8을 아예 빼도 문제 없음. 오히려 지금 상황에서는 그게 더 깔끔할 수 있는 이유가 Recommend Assignee/Verifier가 PL의 독립 목표가 아니라 Assign Issue 안에서 시스템이 제공하는 보조 계산이고 UCD는 actor goal 중심으로 그리는 산출물이라, 모든 내부 보조 기능을 반드시 UC로 뽑을 필요가 없음. 추천은 UC5 Assign Issue 명세의 main flow나 note에 넣어도 충분

이유: UC5-RV, 즉 verifier 교체에는 assignee 추천이 필수가 아니기 때문 --> verifier 교체시에도 추천을 해준다면?
UC8을 이렇게 재정의하면 달라짐.

UC8 Recommend Assignee/Verifier
또는 더 일반적으로:

UC8 Recommend Assignment Candidate

NEW -> ASSIGNED:
  Dev assignee 후보 + Tester verifier 후보 추천

REOPENED -> ASSIGNED:
  Dev assignee 후보 + Tester verifier 후보 추천

ASSIGNED -> ASSIGNED(Assginee 변경):
  Dev assignee 후보 추천

FIXED -> FIXED(tester 변경):
  Tester verifier 후보 추천

UC5 includes UC8
UC8 = Recommend Assignment Candidate

assignee 배정/변경 branch: Dev 후보 추천
verifier 지정/변경 branch: Tester 후보 추천
둘 다 필요한 branch: Dev + Tester 후보 추천

어 아니다
아님
윤동이 방법이 맞음

이때 시스템은 fixed된 이
슈들의 이력을 이용해서 가정 적절한 개발자를 추천해줌. (예를 들어 “best candidate: dev2,
dev5, dev1 등으로”, 가장 가능성이 높은 후보 3명을 순서대로 추천함)

이거 한명만 추천하는게 아니라 여러 명 추천하는방식인데
그건 근데 설계랑 좀 벗어나긴함
차라리 이렇게 가는거지
일반 추천:
dev1
dev2
dev3
reopen 추천:
dev1 - 기존 dev
dev2
dev3
UC5 - RV 추천:
dev1 - 원래 dev
dev2
dev3
이런식으로

그.... 일단 UC5 세분화 여부랑 UC8 어케할지 다시 얘기 하는게 어떨까요

저는 기본적으로 다 추천하는게
깔끔한거같아요

나도 과제 지시서에 여러명 추천하라고 나와있으니까 차라리 그러면 그 여러 명 중 한명을 기존 assignee나 verifier로 하면 될것같은데

그러면 뭐 맨 위에 추천목록만 기존 사람으로 하고 (없으면 없는거고) 나머지부터 1순위 2순위... 하면 될듯

UC5-RA,RV는 그냥 UC8 쓴다고 언급만 하면 됨
reopen -> assign을 UC5로 편입 가능
include 관계 유지됨(바꿀거 없음)
ㅇㅇ 그래서 UC8이 issue, issueHistory 2개에 종속성 생기는거지
근데 issue에 종속성 생기는건 크게 문제 없을듯 어차피 상태 겹치는 경우는 없으니까

그치

이중 1번은 UC5-RA, RV에 UC8이 include 관계가 아니다
ㄴ> 여기에 추가로 UC8은 이슈 status를 보고 추천을 다르게 한다
이러면 문제 4개 전부 해결 완료
이거 그리고 웬만하면 작업 스레드 하나에서 하는게 좋음
시간정보에 따라 컴팩션되는게 베스트옵션이라

##정리
 - 위 회의 내용이 더 많지만, 내용을 축약해서 다음과 같이 정리해보았다:
1) UC5 includes UC8 문제
- UC8 Recommend Assignee를 UC8 Recommend Assignment Candidates로 변경.
- UC8은 Issue.status에 따라 assignee/verifier 후보를 다르게 반환하는 보조 UC로 정의.
- UC5 -> UC8 <<include>>는 유지.
- UC5의 모든 branch에서 UC8을 호출한다고 명시.


2) ASSIGNMENT_CHANGED enum 문제
- Domain Model의 ActionType enum에 ASSIGNMENT_CHANGED 추가.
- ASSIGNED -> ASSIGNED assignee 변경, FIXED -> FIXED verifier 변경 이력에 사용.
- STATUS_CHANGED는 실제 status 값이 바뀌는 전이에만 사용.


3) UC10 role top-level input 문제
- viewStatistics(period, role, filters)를 viewStatistics(period, filters)로 수정.
- filters.scope 또는 statisticsScope로 전체/직군별 통계 범위를 표현.
- currentProject, currentUserRole은 사용자 입력이 아니라 시스템/auth context에서 결정한다고 note 추가.


4) resolver 동기화 문제
- Domain Model에 User resolves Issue 연관 또는 Issue.resolver를 공식 유지.
- note에 verifier = 배정된 검증자, resolver = 실제 FIXED -> RESOLVED 수행자라고 명시.
- UC6/SSD/OC에 FIXED -> RESOLVED 시 resolver=current Tester, assignee/verifier/fixer 유지를 반영.
- RESOLVED -> CLOSED에서만 assignee/verifier=null, fixer/resolver 보존으로 정리.


5_ UC5-RA/RV 분리 여부
- 별도 UC로 분리하지 않음.
- UC5 내부 branch/alternative flow로 유지.
- UC5 상태별 branch:
  * NEW -> ASSIGNED
  * REOPENED -> ASSIGNED
  * ASSIGNED -> ASSIGNED assignee 변경
  *FIXED -> FIXED verifier 변경
  => SSD 05는 이 네 branch를 alt로 표현.

##지시문
위 사항들을 반영해야 한다. 전부 살펴봐라. 또한, resolver 필드 추가 및 이에 따른 변경점들(Changes.docx 문서)은 중요하니, 꼼꼼하게 봐라.
이 문서를 전부 학습한 후, 최종적으로 conflict 지점과 수정점들을 뽑아낼 것이다. 우선, 수정 포인트를 판단하기 전에, **의견 충돌 지점**부터 찾아야 한다. 의견이 합일되었다고 판단한다면(컨텍스트와 이 문서의 결정내용을 기반으로 판단했을 때) 이 수정사항을 적용한다면 발생할 수 있는 문제점을 분석한다.

두 가지로 정형화해서 대답한다:
1. 의견 충돌 지점
2. 이 문서 적용 시 발생 가능한 문제점





















