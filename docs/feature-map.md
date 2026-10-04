# 기능과 검증 범위

## agent-engineering 적용 경로

대표 경로는 **합성 공급자 HTTP 수집 → H2 commit → 실제 검색/상세/추천 HTTP**다. 사용자는 가용 상태여도 관측 시각이 없으면 추천을 확정하지 않고 확인 필요 그룹과 이유를 받는다.

- 준비: JDK 25, Python 3, 합성 `src/test/resources/publicdata/normal.xml`, loopback의 동적 포트. 테스트가 공급자·Clock·데이터 정리를 소유하므로 공공 API 키나 seed SQL이 필요하지 않다.
- 실행: `./gradlew test --tests '*Recommendation*Tests' --tests '*StationReadFlowTests' --tests '*Architecture*Tests'`. 최종 공용 검증은 `bash scripts/verify.sh`; 현재 소스 증거 계획은 [.agents/verification.json](../.agents/verification.json).
- 관찰: 재수집 후 충전소·충전기 각 1건, HTTP 200, `preferred=[]`, `requiresConfirmation`의 해당 충전소에 `UNVERIFIED_AVAILABLE`. 공급자 503 후에도 이전 저장 내용·마지막 성공 시각과 확인 필요 추천을 유지한다.
- 책임: StationSyncService는 수집 순서, StationUpsertService는 페이지 저장, Station/Charger는 상태 불변식, StationQueryService는 조회, FreshnessPolicy는 관측 최신성, CandidateCharger/CandidatePolicy는 제외 이유와 추천 분류, Controller/DTO는 HTTP 변환을 소유한다.
- 경계: 실제 Controller의 Repository 직접 의존을 기존 ArchUnit이 거부한다. 바뀐 소스의 이전 검증 증거는 전역 증거 도구가 거부한다. 규칙 의미와 소유권 전체는 리뷰를 함께 수행한다.
- 근거: `build/verification/station-read-flow/*.json`, JUnit XML과 [적용 기록](agent-engineering-application.md). 실공급자 인증·H2 재시작 보존은 별도 미확인이다.

## CI 구조·누락 검사

도메인 규칙의 결과는 기존 동작 테스트가 맡는다. 의존성·공개 변경 경로는 [구조 검사](architecture-guardrails.md)가 맡고, 실제 규칙 소유권·이름·책임은 리뷰가 함께 확인한다.

- ArchitectureTests 6건: 운영 객체 4개 경계와 Service/MVC 테스트 2개 경계.
- ArchitectureRulesTests 17건: 허용·금지 구조의 bytecode 예제. 역할별 domain 패키지의 추천 정책 선택 범위도 확인한다.
- Python 검사기 테스트 8건: 필수 suite 누락, 새 suite 허용, 보고서 없음·0개·빈 suite·실패·오류·skip 거부.
- [필수 suite 목록](required-test-suites.json): T10~T13을 포함한 기능 30개 + 구조 검사 2개. 실제 XML에 모두 실행되어야 한다. 고정 총건수·coverage 비율은 강제하지 않는다.
- Red/Green·실제 임시 위반·한계: [CI 보강 기록](ci-boundaries-verification.md). JaCoCo는 사용자 선택으로 제외했다.

| 경로 | 기대 결과 | 검증 |
| --- | --- | --- |
| GET /actuator/health | 200, status UP, 세부 정보 미노출 | 실제 HTTP 통합 테스트 |
| GET /actuator/env | 404 | 실제 HTTP 통합 테스트 |
| JPA 저장 → flush → context clear → 조회 | DB에서 동일한 내용을 새 객체로 조회 | H2 + 실제 Repository 테스트 |
| API 문서 생성 | 응답의 필드 설명이 일치해야 생성 성공 | HealthDocumentationTests + REST Docs |
| 패키징 JAR의 GET /docs/index.html | 생성된 HTML과 같은 문서, 200 | 실행 JAR HTTP 확인 |

Station/Charger 업무 엔티티와 Repository·upsert는 T04에서 구현했다. 업무 조회 API는 T08/T09에서 구현했다. T02의 생성 시 검증하는 도메인 모델은 아래 테스트로 확인한다.
문서 테스트는 MockMvc 기반이며 실제 네트워크 테스트와 구분한다. 실제 HTTP 검증은 별도 테스트와 JAR 실행으로 수행한다.

문서에 status 설명을 누락시키면 REST Docs 테스트가 실패하는 것을 확인했다.
메모리 H2는 실행 중 데이터만 유지하며 재시작 후 보존은 검증 대상이 아니다.

개발 전달 경로: [작은 기능별 PR·검증·자동 머지](delivery-workflow.md). 공용 명령 `bash scripts/verify.sh`는 테스트 결과·패키징·실제 HTTP를 검사한다. GitHub 보호 규칙과 원격 CI의 실제 적용 여부는 로컬 성공과 별도로 확인한다.

실행 절차: [verify-plugpass](../.agents/skills/verify-plugpass/SKILL.md)
이번 실행 기록: [JPA·REST Docs 검증](jpa-restdocs-verification.md)

| T02 도메인 경로 | 기대 결과 | 검증 |
| --- | --- | --- |
| ChargerId 생성·Set 등록 | 세 식별자의 차이는 구분, 동일 값 중복 판정, 선행 0 보존 | StationModelTests, Spring/DB 없는 단위 테스트 |
| GeoPoint 생성 | 범위 양끝 포함, 바로 밖·NaN·무한대 거부 | StationModelTests |
| Station/Charger 생성 | 빈 식별자·이름, null 위치·ID 거부 | StationModelTests |

이 검증은 공공 API 인증 성공·충전소 JPA 저장·업무 HTTP 흐름을 증명하지 않는다.

| T03 경로 | 기대 결과 | 검증 |
| --- | --- | --- |
| 외부 상태 코드 → ChargerStatus | 공식 코드 매핑, 미지원/null은 UNKNOWN | ProviderStatusMapperTests 19건 |
| Charger 상태·원본 보존 | 정규화 값과 원본 분리, null 업무 상태 거부 | ProviderStatusMapperTests |

| T04 경로 | 기대 결과 | 검증 |
| --- | --- | --- |
| 엔티티 저장/매핑 복원 | ID·위치·enum 문자열·운영 정보·원본 시각 복원, unique 제약 | StationPersistenceTests 3건 |
| Service upsert → commit → 재조회 | 중복 방지·갱신·역순 거부·실패 rollback | StationUpsertTests 7건 |
| 도메인 갱신 / Snapshot 생성 | 실패 후 상태/시각 유지·관계 방어·필수값 거부 | StationMutationTests 4건, StationSnapshotTests 8건 |

| T05 경로 | 기대 결과 | 검증 |
| --- | --- | --- |
| 실제 fixture HTTP → Snapshot | 식별·위치·상태·운영/원본 시각 보존, 페이지 종료 | PublicDataClientTests |
| 인증·서버·XML·설정·timeout 실패 | 오류 분류·키 비노출·헤더/본문 지연 종료 | PublicDataClientTests |

공공 API 실인증은 미확인이다. 클라이언트는 T06의 DB 수집 실행에 연결됐으며 T11의 기본 비활성 스케줄과 T12의 실행 예산·제한 재시도에 연결됐다.

## T06 수집 실행

StationSyncService → PublicDataClient → StationUpsertService.upsertPage → 실제 H2 commit.
SyncRun은 실행 상태와 실패 페이지/코드를 소유한다. StationSyncTests 12건·SyncRunTests 10건으로
순차 처리·부분 실패·반복 실행·rollback·시각과 상태 방어를 확인한다. 상세 근거: [2주차 기록](week2-verification.md).

## T07 최신성

FreshnessPolicy.evaluate/assess는 sourceObservedAt과 now만 받아 Freshness·이유를 반환한다.
FreshnessPolicyTests 14건: 경계·누락·미래·재수집·설정 방어. 실제 공급자는 sourceObservedAt=null → UNVERIFIED.

## T08 주변 조회

GET /api/v1/stations → StationSearchRequest → StationQueryService → 저장 데이터/성공 이력 → StationSearchResponse.
Connector는 조합 호환, GeoPoint는 직선거리, FreshnessPolicy는 관측 시각 판단을 소유한다.
StationSearchTests7·StationSearchHttpTests20·StationSearchQueryTests6·ConnectorTests12·GeoDistanceTests4 =49건.
Service 실제 DB와 MVC slice 계약을 구분한다. 공용 verify는 실행 JAR의 검색200·validation400도 확인한다.

수집 완료는 마지막 페이지 도달과 실제 전체 처리 건수 일치를 함께 요구한다. 불일치는 CONTRACT 부분 실패로 기록한다.

## T09 상세와 실제 연결

GET /api/v1/stations/{stationId} → StationQueryService.detail → 실제 Station/Charger 조회 → StationDetailResponse.
StationDetailTests5·StationDetailHttpTests7·StationReadFlowTests2 =14건.
실제 fixture HTTP 수집부터 H2와 실제 검색/상세 HTTP, 공급자503 중 기존 정보 조회를 확인한다.
공용 verify는 실행 JAR의 없는 상세404 공개 오류도 검증한다. 실제 공공 API 인증과 cmux 화면 검증은 별도 미확인이다.

실패 페이지 건수를 failedPageCount로 명시 저장·반환한다. 리뷰 보완 후 T06 관련22건, 전체260건이다.

회차 내 중복 ID 페이지는 저장 전에 거부한다. SyncProgress가 순서·헤더·중복·완료 건수를 소유한다.
[리뷰·리팩터링 검증 기록](code-review-2026-10-02.md)에 Red/Green/Refactor와 한계를 기록했다.

## T10 후보 추천

GET /api/v1/recommendations → RecommendationRequest → RecommendationService → 반경 내 저장 데이터 → CandidatePolicy → 응답 DTO.
RecommendationTests14·RecommendationServiceTests3·RecommendationHttpTests9·RecommendationRouteTests1, 전체287건.
fixture HTTP부터 추천 HTTP까지 관측 시각 누락의 UNVERIFIED 분리를 확인한다. 그룹별 limit 전에 판단·제외하며 각 그룹은 거리/ID 순서다.
근거와 한계: [3주차 기록](week3-verification.md).

## T11 반복 수집

기본 비활성화. 명시적 설정+키 → SyncScheduler 고정 지연 → StationSyncService 실행 소유권 → 기존 수집 경로. SyncSchedulerTests6건, 전체293건. 예정/직접 중복 호출은 SKIPPED, 예외 후 다음 실행 가능. 단일 프로세스 범위다.

## T12 장애 제한과 복구

회차 IngestionBudget → 제한 재시도 → PublicDataClient의 남은 시간 timeout → 페이지 commit/종료 이력.
IngestionRecoveryTests14·IngestionHttpBudgetTests6·IngestionConfigurationTests11, 전체324건.
일시 오류만 한 번 재시도, 인증/계약/interrupt는 즉시 종료. 페이지·전체요청·시간 상한과 Retry-After 예산, 부분 commit 보존, 시간 경과 STALE·다음 회차 복구를 실제 Service/DB·fixture HTTP로 확인한다.

## 패키지 책임과 탐색

기능별 묶음 안에서 domain·repository·service·controller·dto를 구분한다. [변경 검증](package-structure-verification.md)에 동작 보존 결과를 기록한다. 검색·추천의 공통 Connector는 station.domain, 공통 숫자 검증은 common.validation이 소유한다.

## T13 운영 관측

페이지·실행 commit → IngestionMetrics.recordCompletion → 고정 태그 Counter·안전한 종료 로그.
DB 관측 시각/성공 이력 + 현재 Clock → Gauge → 명시적으로 노출한 Actuator metrics HTTP.
IngestionMetricsTests6건, 도입 시 전체330건·CI 구조 검사 통합 후 전체353건. 성공/부분 실패/실패·재시도·마지막 성공 시각 경과·최신성 경계/누락/미래/시간 경과·로그 비밀값 비노출을 확인한다.
관측 연결은 `ingestion.metrics`가 소유하고 도메인·서비스·저장소의 기존 책임은 유지한다. [운영 확인 명령](operations.md), [3주차 근거](week3-verification.md).
