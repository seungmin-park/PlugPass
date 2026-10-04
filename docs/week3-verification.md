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

- T10 PR [#19](https://github.com/seungmin-park/PlugPass/pull/19), 필수 CI [성공](https://github.com/seungmin-park/PlugPass/actions/runs/37220612868), 실제 main `6c40b0b`.

## T11

- Red: `./gradlew test --tests '*SyncSchedulerTests'` 5건 중4 실패. 중복 호출이 대기함, 예외 실행이 RUNNING으로 남음, 인증 없는 활성화·짧은 간격이 허용됨을 assertion으로 확인했다.
- Green: 실행 소유권은 Service의 AtomicBoolean이 공유한다. 예정/직접 호출 모두 진입 전에 획득하고 finally에서 반환한다. 중복은 이력/네트워크를 만들지 않는 SKIPPED 결과다. 예외는 INTERNAL 종료 이력을 남기고 호출자에게 전달한다. 스케줄러는 비밀값 없이 경고하고 다음 회차를 유지한다.
- 고정 지연은 이전 실행 종료 이후 기다린다. 최초 실행도 설정 간격 뒤에 시작한다. 기본 비활성화·명시적 활성화+키·최소30분을 적용한다. 20요청/회차 상한은 T12에서 연결한다.
- Refactor: 기존 synchronized의 대기 큐를 실행 소유권으로 바꿨다. 다른 상태와 JPA 트랜잭션은 기존 경계를 유지한다. 소유권은 한 Service bean/한 프로세스만 보장하며 분산 락은 없다.
- 이름/책임: Scheduler는 실행 시점, Service는 실행 허가·수집 흐름, SyncRun은 이력 불변식이다. 제어 가능한 외부 I/O·latch로 경합/예외 회복을 검증하며 장시간 sleep은 없다.

- 전체 verify 종료0,293건 실패/오류/skip0. SyncSchedulerTests6건은 실제 Service/DB·기본 비활성화·키 없는 활성화 거부·키 있는 고정 지연 등록·간격 방어를 확인했다.

- T11 PR [#20](https://github.com/seungmin-park/PlugPass/pull/20), 필수 CI [성공](https://github.com/seungmin-park/PlugPass/actions/runs/37220900851), main `8607e46`.

## T12

- Red: Recovery14건 중8건 assertion 실패(재시도·페이지·요청·시간 상한 누락). HTTP6건 중3건 실패(남은 시간 대신 CONTRACT, Retry-After 값 누락). 설정11건 모두 거부 assertion 실패. 명령은 `./gradlew test --tests '*IngestionRecoveryTests'`, `*IngestionHttpBudgetTests`, `*IngestionConfigurationTests`; 로그 `/tmp/plugpass-week3/t12-*-red.log`.
- Green: 회차 소유 IngestionBudget은 monotonic 시간·페이지·전체 요청을 제한하고 RetryingPageFetcher는 일시 오류만 한 번 재시도한다. 인증/계약/예산 오류·interrupt는 반복하지 않는다. Retry-After 초·HTTP-date를 읽고 잘못된 값은 버린다. 대기가 남은 예산 이상이면 재시도하지 않는다. DefaultPublicDataClient는 응답 timeout과 남은 회차 시간 중 작은 값으로 헤더/본문 전체 future를 취소한다.
- 기본10페이지/20요청/1재시도/120초/대기1초. 상한을 높이는 설정은 거부해30분 스케줄의 최대960요청 계산을 유지한다. 수동 회차와 재시작 횟수는 이 일일 계산에 포함되지 않으므로 실제 계정 quota 확인 전 기본 비활성화를 유지한다.
- 대상31건 Green 후 전체324건 실행에서 기존2건 실패: T06 fixture의 timeout1개가 재시도에 소진, T09 실제 HTTP 횟수 기대2→3. timeout fixture2개와 요청1,2,2/전체3회 assertion으로 현재 계약을 명시했다. 기존 commit/부분 실패/조회 보존 assertion은 유지했다. 전체 재실행·공용 verify324건 실패/오류/skip0, JAR HTTP·문서 일치.
- Refactor: 모든 PublicDataClient 구현이 남은 Duration을 받도록 필수 인터페이스 계약으로 정리했다. 운영 구현은 실제 timeout을 적용하고 외부 fake만 제어된 실행으로 대체한다. 기존 스케줄/직접 호출은 같은 예산을 사용한다. 객체/메서드 이름·호출부·반환값·저장 필드를 함께 검토했다.
- 예산은 외부 요청/재시도 대기를 제한한다. DB 저장을 강제 중단하는 실시간 타이머나 프로세스 재시작 후 복구는 아니다. 장애 중 기존 데이터는 유지되지만 관측 시각이11분 지나면 STALE, 공급자 다음 정상 회차에서 같은 ID가 갱신돼 RECENT로 복구함을 실제 Service·DB로 확인했다. TS/프런트엔드는 해당 없음.

- Refactor 후 공용 verify 종료0,324건 실패/오류/skip0, JAR HTTP·문서 일치.

- main에 패키지 구조 PR #21(`789ba21`)이 먼저 반영되어 보호 규칙 strict 조건상 T12가 BEHIND가 됐다. 최신 main을 병합하고 양쪽 문서·기능을 유지했다. T12 설정은 config, 시간/HTTP 경계는 client, 회차 조정은 service로 이동하며 import·테스트 위치를 함께 갱신한다. 공개 HTTP/DB 계약은 유지하고 전체 검증·CI를 다시 실행한다.

- 최신 main 충돌 해결 뒤 공용 verify324건 실패/오류/skip0, 실제 JAR HTTP·문서 일치. 구조 변경에 따른 컴파일·Spring 탐색·계약 보존을 확인했다.

- T12 PR [#22](https://github.com/seungmin-park/PlugPass/pull/22), 최신 문서 main도 병합한 필수 CI [성공](https://github.com/seungmin-park/PlugPass/actions/runs/37222055266), 실제 main `c1e4cd2`. 문서 정리 PR #24는 코드 변화 없어 문서 참조를 검사했다. 머지 신청의 잘못된 head 값은 거부됐으며 실제 local/remote SHA를 다시 조회해 바로잡았다. 보호를 우회하지 않았다.

## T13

- Red: `./gradlew test --tests '*IngestionMetricsTests'` 6건 모두 assertion 실패. 성공/부분 실패/실패 Counter와 경과 Gauge/종료 로그가 없었고 실제 품질 metrics HTTP가404였다. `/tmp/plugpass-week3/t13-red.log`.
- Green: 실제 실행 이력 commit 뒤 Counter와 runId/처리 건수/실패 코드/요청·재시도 로그를 연결했다. 원본 예외·키·본문·URL을 출력하지 않고 태그는 outcome/cause/freshness만 사용한다.
- 마지막 성공 경과는 SUCCESS 이력을 조회하므로 실패/부분 실패가 초기화하지 않는다. 최신성은 FreshnessPolicy의 경계와 DB count를 사용해 현재값을 계산한다. 실제 health200/UP와 동시에 최신2·오래됨1·미확인2의 지표 HTTP200을 확인했다. 해당 출력은 `build/verification/ingestion-metrics/`에 남긴다.
- 대상6건·전체 공용 verify330건, 실패/오류/skip0. 기존 REST Docs·JAR HTTP·문서 일치도 통과했다. 시간 경과 품질 분포와 실제 경과 metrics HTTP를 추가 확인한다.
- 책임 검토: `ingestion.metrics`는 관측 등록·종료 기록을 맡는다. 도메인의 최신성 규칙을 복제하지 않고 경계 시각을 받아 DB count한다. Service가 실행을 조정하고 Metrics는 DB를 바꾸지 않는다. 현재 관측 규모가 작고 외부 전송이 없으므로 추가 위임 객체 없이 유지한다. 다섯 협력 의존성과 record/DTO/JPA 형태는 이 역할의 좁은 예외로 유지하며 실제 DB·HTTP가 보완 검증이다. TS/프런트엔드는 없음.
- 외부 전송·유료 모니터링·분산 수집은 해당 없음. 카운터는 프로세스마다 초기화되고 각 Gauge의 조회 시점은 달라 동시 수집 중 원자적 묶음 snapshot은 보장하지 않는다. [운영 문서](operations.md)에 확인 명령·단위·실패별 해석을 기록했다.

- 보강한 최종 공용 verify 종료0,330건 실패/오류/skip0. 11분 경과 후 RECENT0/STALE4/UNVERIFIED1, 실제 마지막 성공 경과 HTTP120초, 기본 JAR metrics404·추천200·문서 일치를 확인했다. Refactor는 불필요해 관측 경계의 현재 구조를 유지했다.

- main의 CI 구조 검사 PR #23(`56aa63e`)을 통합했다. 양쪽 검증 안내를 유지하고 T13 핵심 suite를 필수 목록에 등록했다. `bash scripts/verify.sh` 종료0: Java353건(업무330+구조23), Python8건, 실패/오류/skip0. 실제 JAR 추천200·기본 metrics404·문서 일치도 다시 확인했다. 로그: `/tmp/plugpass-week3/t13-main-integration-verify.log`, 결과: `build/verification/runtime.json`.
- 통합 검증 시 cmux를 다시 확인했으나 호출 workspace/surface가 없고 “cmux 내부에서 시작된 프로세스만 연결 가능”으로 접근이 거부됐다. 기존 live socket 부재와 함께 현재 실행에서 화면 E2E를 확인하지 못한 원인으로 기록한다.
