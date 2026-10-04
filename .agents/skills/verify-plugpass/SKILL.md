---
name: verify-plugpass
description: Verify PlugPass HTTP health, H2 JPA persistence, REST Docs contracts, and packaged API documentation.
---

# PlugPass 검증

## 환경 확인

- 프로젝트 루트에서 `java -version`으로 JDK 25를 확인한다.
- `CMUX_WORKSPACE_ID`, `CMUX_SURFACE_ID`, `cmux identify --json`으로 호출한 세션을 확인한다.
- 현재 cmux의 검증 보조 pane을 재사용하고 workspace·surface를 명시한다. 사용자가 입력 중인 터미널은 사용하지 않는다.
- cmux 연결이 없으면 그 한계를 먼저 설명하고 터미널 검증을 화면 E2E라고 보고하지 않는다.

## 빌드와 테스트

1. `bash scripts/verify.sh`를 실행한다. clean build와 아래 JAR·실제 HTTP 검사를 같은 명령으로 수행한다. JDK 25와 Python 3이 필요하다. CI도 이 명령을 사용한다.
2. 종료 0과 `build/test-results/test/TEST-*.xml`의 테스트 324개(T12 31개 + T11 6개 + T10 27개 + 기반 4개 + T02 52개 + T03 19개 + T04 22개 + T05 64개 + T06 22개 + T07 14개 + T08 49개 + T09 14개), 실패·오류·skip 0을 확인한다. 기능을 추가했다면 기대 개수도 갱신한다.
3. [테스트 경계](../../../AGENTS.md)를 따른다. Repository는 기본 save 후 조회하고 매핑 복원이 검증 대상일 때만 이유를 명시해 flush/clear한다. 참조 차이 자체를 assertion하지 않는다. Service는 테스트 트랜잭션 없이 production commit 후 별도 조회하고 AfterEach로 정리한다. 테스트 전용 엔티티가 실행 JAR에 들어가지 않아야 한다.
4. `build/generated-snippets/health/`와 `build/docs/asciidoc/index.html`이 생성돼야 한다.
5. JAR의 `BOOT-INF/classes/static/docs/index.html`이 생성한 HTML과 같아야 한다.

## 실제 HTTP 확인

`java -jar build/libs/plugpass-0.0.1-SNAPSHOT.jar --server.port=18080`으로 실행한다. 해당 포트가 사용 중이면 빈 포트를 사용한다.

- GET /actuator/health: HTTP 200, JSON status UP, components 없음.
- GET /actuator/env: HTTP 404.
- GET /docs/index.html: HTTP 200, 응답이 생성한 HTML과 동일.

문서 생성 테스트는 MockMvc 기반이다. 실제 네트워크 검증과 구분해서 보고한다.

## 문서 계약 검사

HealthDocumentationTests의 status 필드 설명을 임시로 제거한 뒤 `./gradlew test --tests '*HealthDocumentationTests'`를 실행한다.
SnippetException에 status가 문서화되지 않았다는 실패가 있어야 한다.
반드시 설명을 복원하고 `./gradlew clean build` 전체 검증을 다시 통과시킨다.
이 검사는 문서 계약을 변경하거나 검사 동작을 확인할 때 수행한다.

## 증거와 종료

기능 변경은 Red의 실제 assertion 실패, Green의 최소 구현, Refactor의 이유와 대상·전체 테스트 결과를 기록한다. 성공·예외·실패·엣지 케이스와 이름·책임 검토를 포함한다. 기존 기반 테스트가 새로운 작성 규칙까지 모두 준수한다고 추정하지 않는다. 문서만 변경하면 링크·참조·내용 정합성을 확인하고 애플리케이션 실행과 구분한다.

종료 코드, XML 테스트 결과, 생성 HTML, `build/verification/server.log`와 `runtime.json`을 확인한다. 공용 검증은 테스트 0개·실패·오류·skip을 거부한다.
PR은 `.github/workflows/verify.yml`의 `PlugPass verify` 결과를 확인한다. 로컬 성공을 원격 CI 성공으로 대신하지 않는다.
직접 시작한 서버만 종료하고 cmux 검증 pane은 유지한다.
H2 메모리 데이터의 재시작 보존은 보장하지 않는다. T09의 실제 fixture 수집→H2→검색/상세 HTTP 범위와 실공공 API 인증·추천 포함 후속 흐름을 구분한다.

## T01/T02 추가 범위

- T01 계약·XML fixture는 docs/data-contract.md에 출처와 합성 여부를 기록한다. 키가 없으면 실응답 완료로 체크하지 않는다.
- T02 반복 검증: `./gradlew test --tests '*StationModelTests'` → 52건, 실패·오류·skip 0.
- T02 완료 시 전체 검증은 56건이었다. 범위는 ID 충돌·동일 ID 중복·선행 0·식별자 누락·좌표 경계/NaN/무한대·모델 필수값.
- Station/Charger는 아직 JPA 엔티티가 아니며 업무 API도 없다. Lombok/엔티티 구성과 저장 검증은 T04 범위다.

T03 반복 검증: `./gradlew test --tests '*ProviderStatusMapperTests'` → 19건. T03 전체는 75건이다.

## T04 추가 검증

`./gradlew test --tests '*StationPersistenceTests' --tests '*StationUpsertTests' --tests '*StationMutationTests' --tests '*StationSnapshotTests'`: 22건. 전체 97건.
Station/Charger는 이제 실제 업무 엔티티다. 앞의 T02/T03 설명은 당시 범위의 기록이며 현재 모델은 Lombok Builder/getter와 JPA로 갱신했다.
Service commit 후 별도 재조회·AfterEach FK 역순 정리·중복 제약·rollback을 실제 테스트로 확인한다.

## T05 추가 검증

`./gradlew test --tests '*PublicDataClientTests'`: 64건. 전체 161건.
실제 loopback HTTP fixture에서 페이지·정규화·키 비노출·헤더/본문 timeout·XML 거부를 확인한다.
실제 공공 API 인증은 키 없어 미확인이다. 수집 실행/스케줄/업무 HTTP는 아직 없다.

## T06

`./gradlew test --tests "*StationSyncTests" --tests "*SyncRunTests"` → 20건. 실제 페이지 commit·실패 rollback·실행 이력을 확인한다.

## T07

`./gradlew test --tests "*FreshnessPolicyTests"` → 14건. 관측 시각만으로 판정하며 다른 시각으로 대체하지 않는다.

## T08

검색 관련49건: StationSearchTests7·StationSearchHttpTests20·StationSearchQueryTests6·ConnectorTests12·GeoDistanceTests4.
공용 verify는 실행 JAR의 검색200과 validation400을 추가 확인한다.

## T09

StationDetailTests5·StationDetailHttpTests7·StationReadFlowTests2 →14건. 실제 fixtureHTTP→수집→H2→검색/상세와 공급자503 중 기존 정보 조회를 확인한다.
`build/verification/station-read-flow/*.json`에 실제 응답을 남긴다. 공용 verify는 실행 JAR 상세404 코드/메시지도 검사한다.

실패 페이지 건수는 failedPageCount로 저장하며 입력 처리 레코드 수와 단위를 구분한다.

## 2주차 코드 리뷰 보완

StationSyncTests12건·SyncRunTests10건으로 수집 관련22건, 전체260건이다.
회차 내 중복 ID가 있는 페이지는 저장 전에 CONTRACT로 거부하며, 첫 페이지 저장0/중간 실패 이전 commit 보존/재수집 허용을 확인한다.
상세 과정은 docs/code-review-2026-10-02.md를 참고한다.

## T10

`./gradlew test --tests "*Recommendation*Tests"` 27건. 전체287건. 정책·실제 DB·MVC를 구분하며 실제 fixture 수집→추천 HTTP와 JAR 추천200을 함께 확인한다.

## T11

`./gradlew test --tests "*SyncSchedulerTests"` 6건. 전체293건. 수집 기본 비활성화, 키 없는 활성화 거부, 고정 지연 등록, 중복 즉시 SKIPPED·예외 후 회복을 확인한다. 실제 공공 API 스케줄 실행은 키 없어 미확인이다.

## T12

IngestionRecoveryTests14 + IngestionHttpBudgetTests6 + IngestionConfigurationTests11 =31건. 전체324건. 제한 재시도·페이지/요청/시간 상한·Retry-After·interrupt·부분 저장 보존·STALE 경과와 다음 회차 복구를 검증한다.

## 패키지 구조 정리

기능 아래 domain·repository·service·controller·dto로 나뉜다. 검색 서비스 예: `./gradlew test --tests 'com.plugpass.search.service.StationSearchTests'`; 기존 클래스 이름 wildcard 명령도 유지한다. 전체 연결 테스트는 search.integration과 recommendation.integration에 있다. 공유 Connector는 station.domain, FiniteDouble은 common.validation이다. 검증 기록은 docs/package-structure-verification.md를 참고한다.
