---
name: verify-plugpass
description: Verify PlugPass behavior and architecture checks, fixture ingestion through H2 to search/detail/recommendation HTTP, packaged REST Docs, current-source evidence, and PR delivery state.
---

# PlugPass 검증

## 환경 확인

- 프로젝트 루트에서 `java -version`으로 JDK 25를 확인한다.
- 일반 verify는 Python 3을 사용한다. 전역 증거 도구를 사용할 때는 해당 Python이 3.10 이상인지 확인한다. JAVA_HOME을 설정하면 그 Java도 25여야 한다.
- `CMUX_WORKSPACE_ID`, `CMUX_SURFACE_ID`, `cmux identify --json`으로 호출한 세션을 확인한다.
- 현재 cmux의 검증 보조 pane을 재사용하고 workspace·surface를 명시한다. 사용자가 입력 중인 터미널은 사용하지 않는다.
- cmux 연결이 없으면 그 한계를 먼저 설명하고 터미널 검증을 화면 E2E라고 보고하지 않는다.

## 빌드와 테스트

1. `bash scripts/verify.sh`를 실행한다. clean build와 아래 JAR·실제 HTTP 검사를 같은 명령으로 수행한다. JDK 25와 Python 3이 필요하다. CI도 이 명령을 사용한다.
2. 종료 0과 `build/test-results/test/TEST-*.xml`의 테스트 360개(T16 3개 포함), 실패·오류·skip 0을 확인한다. Python 검사기·성능 집계 테스트 21개도 실행되어야 한다. 기능을 추가했다면 실행 기록의 관측 건수를 갱신한다. 고정 총건수를 CI의 요구값으로 사용하지 않는다.
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

## 현재 소스와 실행 근거 연결

전역 agent-engineering이 설치된 환경에서 [검증 계획](../../verification.json)을 사용한다. 새 검사기가 아니라 기존 `bash scripts/verify.sh`의 실행 계획이다. 동작·구조·필수 suite·JAR HTTP 판단은 기존 검사들이 맡고, 전역 `evidence.py`는 명령·로그와 소스의 일치를 기록한다.

Python 3.10+와 JDK 25 환경에서 프로젝트 루트의 아래 명령을 실행한다. 출력 폴더는 저장소 밖의 새 경로여야 한다.

```bash
plugpass_evidence_dir="$(mktemp -d "${TMPDIR:-/tmp}/plugpass-evidence.XXXXXX")/proof"
python3 ~/.agents/skills/agent-engineering/scripts/evidence.py run \
  --repo . --plan .agents/verification.json --out "$plugpass_evidence_dir"
python3 ~/.agents/skills/agent-engineering/scripts/evidence.py check \
  --repo . --plan .agents/verification.json --out "$plugpass_evidence_dir"
```

둘 다 종료 0이어야 한다. plan은 exit-only 기록이므로 증거 도구가 JUnit을 직접 세었다고 보고하지 않는다. 실제 실행 건수·실패·오류·skip·필수 suite는 `verify-runtime.py`와 XML·`build/verification/runtime.json`에서 확인한다. 전역 도구는 요구사항 완전성이나 assertion 품질을 증명하지 않는다.

commit·같은 HEAD의 파일 변경·plan 변경 후에는 이전 증거가 현재 검증을 대신할 수 없다. 최종 commit의 현재 내용을 검증하고, 실패 로그를 보존한 채 원인에 맞춰 수정·재검증한다. runtime·의존성·환경·외부 상태의 식별은 별도로 기록한다. [대표 흐름과 실제 거부 확인](../../../docs/agent-engineering-application.md)을 참고한다.

## 현재 PR의 완료 확인

[전달 절차](../../../docs/delivery-workflow.md)에 따라 실제 main 보호 정책과 검사 출처를 먼저 확인한다. `PlugPass verify`라는 이름만으로 보호 정책이나 테스트 실행을 추정하지 않는다. 전역 `pr_status.py`로 검증한 commit과 현재 PR head, 필요한 검사, native 머지 상태를 읽기만 할 수 있다.

검사 실패·대기·누락/skip·head 변경·머지 차단을 구분한다. CI 성공, 머지 가능, 실제 머지를 별도 관찰로 기록한다. 요청 범위와 프로젝트 정책이 실제 머지까지 요구하면 `--goal merged`로 최종 상태를 확인한다. ready나 auto-merge 신청만으로 완료 처리하지 않는다. 실행 명령은 전달 절차에 있다.

필수 승인 인원은 현재 0명이며 구조 검사가 의미적 책임 리뷰를 대신하지 않는다. 도메인 규칙의 정확성은 동작 테스트가, 책임 배치는 구조 검사와 리뷰가 맡는다. 스킬 지침 검토·도구 테스트·실제 에이전트 행동 평가도 구분한다. 이 검증 스킬은 에이전트 병렬화나 추가 배포 권한을 주지 않는다.

## 구조와 필수 suite 검사

- `./gradlew test --tests '*Architecture*Tests'`: 23건. [자동 검사 범위와 리뷰 경계](../../../docs/architecture-guardrails.md)를 따른다. 도메인 규칙의 정확성은 기존 동작 assertion으로 확인한다.
- `python3 -m unittest discover -s scripts -p 'test_*.py' -v`: 21건. 필수 suite 검사8건·HTTP 산출물3건·운영JAR 제외2건·프런트 결과 검사5건·성능 집계3건으로 실패 조건과 정상 입력을 확인한다.
- 공용 verify는 [필수 suite 목록](../../../docs/required-test-suites.json)의 35개 이름이 실제 XML에 모두 있는지 검사한다. 새 기능 완료 시 해당 핵심 suite를 등록하고, 삭제·이름 변경 시 기능 지도와 대체 검증을 함께 리뷰한다. 내부 테스트 일부 삭제·assertion 품질은 이 목록 검사로 보장하지 않는다.
- 검사기 변경 시 허용/금지 bytecode 예제를 확인하고 실제 임시 위반의 거부·복원을 확인한다. 기록: [CI 검증 보강](../../../docs/ci-boundaries-verification.md).
- JaCoCo는 사용자 선택으로 제외했다. 수치 목표용 DTO/getter 테스트나 별도 coverage gate를 추가하지 않는다. 정적 검사는 좋은 이름·불변식 소유권 전체를 판정하지 않으므로 PR의 객체 책임 리뷰를 수행한다.

직접 시작한 서버만 종료하고 cmux 검증 pane은 유지한다.
H2 메모리 데이터의 재시작 보존은 보장하지 않는다. 실제 fixture 수집→H2→검색/상세/추천 HTTP와 품질 metrics HTTP를 확인한다. 실공공 API 인증과 cmux 화면 E2E는 별도 미확인이다.

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

## T13

`./gradlew test --tests "*IngestionMetricsTests"` 6건. T13 도입 시 전체330건, CI 구조 검사 통합 후 전체353건. 성공/부분 실패/실패·요청·재시도·마지막 성공 경과·품질 분포/시간 경과·비밀값 없는 로그를 실제 DB와 metrics HTTP로 확인한다. 기본 metrics404, 명시적 노출의 품질 metrics200을 구분한다. 운영 확인은 docs/operations.md, HTTP 출력은 build/verification/ingestion-metrics/에 있다.

## T14 종합 흐름

`./gradlew test --tests '*ChargingJourneyTests'`3건. 실제 loopback 공급자·랜덤 포트 업무 HTTP로 검색→상세→첫 충전소 제외 추천·재수집·STALE·503·복구·빈 결과를 assertion한다. 관측 시각의 경과는 명시적 합성 관측 시각 범위이며 실제 공급자 계약은 UNVERIFIED를 유지한다.
공용 verify는 필수 suite33개와 build/verification/charging-journey/의 필수 응답7종(search/detail/alternative/stale/failure/recovered/empty)을 검사한다. 전체 관측356건·Python11건. [데모](../../../docs/demo.md)의 cmux/실공공 API 한계를 함께 기록한다.

## T15 성능 기준선

현재 필수 suite34개·Java357건·Python14건. `python3 scripts/performance/load.py --output <새폴더>`로 실제10,000/50,000 생성 데이터·동시20명·warmup30초·측정180초를 별도로 실행한다. 공용 verify만으로 성능 완료를 주장하지 않는다. 작은 데이터 매핑 테스트1건과 집계3건도 유지한다. 부하 전후 정확성·SQL/엔티티 수·p50/p95·unexpected error·대상 SHA/dirty·샘플 원본을 [성능 기록](../../../docs/performance.md)과 비교한다. smoke 설정은 기준선이 아니다. 운영 JAR에서 performance test 패키지가 제외되는지도 공용 검증이 확인한다.


## T17~T19 최신 검증 범위

현재 공용verify는 Java360건·Python21건·프런트144건(10파일)을 실행한다. 과거 T번호의 건수는 당시 기록이다. 실제 실행 뒤 XML/JSON의 현재 수를 읽고 고정 총건수를 CI요구값으로 만들지 않는다.

- T17: 기존 recovery/sync/scheduler/journey35건과 `scripts/verify-memory-restart.py`의 별도JVM3회 실제HTTP로 수집완료→메모리H2소실/준비전→재수집완료를 확인한다. `build/verification/memory-restart/`를 확인한다. reliability 테스트클래스의 운영JAR 포함을 거부한다.
- T18: Node26.7.0, lockfile을 사용한다. 공용verify가 npm ci→test:unit→type-check→lint(경고0)→build를 실행한다. `frontend/test-results/unit.json`의 실제assertion과 `docs/required-frontend-tests.json`의 필수파일을 기존runtime검사로 확인한다. 0개/실패/skip/누락을 거부한다.
- 현재 프런트명령은 `npm --prefix frontend run test:unit`, `type-check`, `lint`, `build`다. 실행은 개발서버 `/app/index.html`·Vite base `/app/`·hashRouter다. JAR웹앱 포함은 T24에 남아 있다.
- 이번 세션은 사용자가 cmux 대신 현재Codex세션을 허용했다. 실제Codex브라우저의 조건선택/클릭/새로고침/없는화면복귀는 `docs/frontend-verification.md`를 따른다. 이것을 T23의 실제API전체E2E·Playwright러너로 바꾸어 보고하지 않는다.

- T19: URL 입력36·ID20·오류14·검색31·상세14·추천18건, 합133건. `npm --prefix frontend run test:unit -- src/features/search/criteria.spec.ts src/shared/api/apiError.spec.ts src/features/search/api/stationApi.spec.ts src/features/station-detail/api/stationDetailApi.spec.ts src/features/recommendation/api/recommendationApi.spec.ts src/shared/api/stationId.spec.ts`로 대상 검증한다. 실제 Axios JSON/취소/직렬화는 유지하고 adapter만 제어한다. UI 검색·JAR 포함 E2E로 표현하지 않는다.
