# 3주차 구현·검증 기록

계획: [plan.md](../plan.md), 실행 항목: [tasks.md](../tasks.md) T10~T13. 시작 main: `85d07bc`.

## 결정과 검증 환경

- 기존 계획의 실행 요청이므로 T10~T13을 순서대로 직접 구현한다. 병렬 에이전트 권한은 없으며 직접 리뷰한다.
- 깨끗한 현재 checkout에서 작업별 기능 브랜치를 사용한다. 추가 worktree는 만들지 않는다.
- 호출 CMUX_WORKSPACE_ID/CMUX_SURFACE_ID가 없고 `cmux identify --json`은 live socket 없음으로 실패했다. 터미널/API 실행은 확인하되 cmux 화면 E2E는 미확인이다.
- Java 25.0.4.1, Boot 4.1.1, 실제 spring-core 7.0.9, micrometer-core 1.17.1. 기존 Actuator 의존성을 재사용한다.
- T10 조회 → 후보 정책: limit 적용 전에 반경 내 전체 후보를 판단해야 가까운 제외 후보가 대체 후보를 가리지 않는다. 기존 검색의 limit 계약은 유지한다.
- T11 실행 소유권 → T12 요청·페이지·시간 예산 → T13 종료 결과·재시도·최신성 지표를 같은 경계에서 연결한다.

## T10

추천 경로 200/빈 그룹을 먼저 검증했다.

- Red: `./gradlew test --tests '*RecommendationRouteTests'` 1건 실패(404 대신200 기대), `*RecommendationTests` 14건 중13 실패(빈 정책의 후보 누락), `*RecommendationServiceTests` 3건 모두 후보 누락 assertion 실패. 로그는 `/tmp/plugpass-week3/t10-*-red.log`.
- Green: 충전기별로 상태·최신성·제한을 함께 판단한다. 하나의 충전기 AVAILABLE과 다른 충전기 RECENT를 합치지 않는다. 조회 Service는 commit된 DB를 읽고 정책은 DB를 모르며 DTO는 HTTP 값만 반환한다.
- 첫 MVC 검증 27건 중1건은 테스트의 NaN 메시지가 기존 공개 계약과 달라 실패했다. systematic-debugging으로 FiniteDouble의 기존 메시지(좌표는 유한수여야 합니다)를 확인해 기대값을 바로잡았다. 기능 Red와 구분한다.
- 결정: limit은 그룹마다 적용한다. 순서는 직선거리→내부 ID이며 제외 ID는 전체 그룹에서 제거한다. 접근/영업 시간 누락은 경고, 자유 형식 영업 시간은 자동 해석하지 않는다.

- 전체 `bash scripts/verify.sh` 종료0: 287건, 실패/오류/skip0. 정책14·Service3·MVC9·실제 빈 경로1 추가. fixture→H2→검색/상세/추천 실제 HTTP와 실행 JAR 추천200을 확인했다.
- Refactor: 기존 검색을 `searchWithinRadius`와 최종 limit으로 나눠 일반 검색 계약을 유지하고 추천의 조기 limit 누락을 막았다. 별도 위임 객체나 새 의존성은 추가하지 않았다.
- 이름→책임→배치 검토: CandidateCharger가 충전기 근거, CandidatePolicy가 그룹·순서, RecommendationService가 조회/제외/limit, Controller와 response가 HTTP 계약을 소유한다. 도메인·DTO record는 관련 값 묶음이며 JPA 엔티티 직접 응답 없음. TS/프런트엔드 없음. 추천에는 외부 I/O·상태 변경이 없어 timeout/rollback/동시 쓰기는 해당 없음; 수집 실패 중 조회는 실제 연결 테스트로 유지했다.
