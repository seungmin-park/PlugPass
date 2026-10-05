# PlugPass 작업 체크리스트

**목표·설계:** [plan.md](plan.md). T01 문서 계약은 main 반영, 인증된 실연동은 키 없어 미확인. T02~T23은 구현·검증·main 반영 완료. T15 당시 p95 목표 미달은 T16 동일 조건 재측정75.04ms로 개선했다. T01 실응답 확인 두 항목은 키 없어 보류한다. 아래 하위 체크와 근거를 따른다.

**실행 방식:** 작업 하나씩 순서대로 진행한다. 구현 시 `superpowers:executing-plans`를 사용한다. 이 파일을 만들었다는 이유로 이후 작업 전체를 자동 실행하지 않는다.

**공통 제약:** Java 25·Spring Boot 4.1.1·JPA/H2·Spring REST Docs, 단일 인스턴스·공급자 1곳·검증 지역 1곳. 공식 계약을 먼저 확인한다. 현재 H2는 메모리 모드다. 작은 PR·필수 CI·자동 머지와 cmux 검증 정책을 유지한다.

**프런트 추가 (2026-10-05):** 사용자는 목록 중심 반응형 웹앱으로 검색·상세·대체 후보를 먼저 완성하고 지도를 후속 단계로 선택했다. [프런트 계획](docs/frontend-plan.md)에 따라 T16→T17→T18~T24 순으로 진행한다. T18 기반·T19 URL/API 계약·T20 실제 주변 검색 화면을 구현했다. 전달 완료는 각 항목의 PR 상태와 검증 근거를 따른다.

## 남은 작업 실행 순서

```text
T16 조회 개선 → T17 장애·복구 검증
  → T18 기반 → T19 API 계약 → T20 검색 → T21 상세 → T22 대체 후보
  → T23 실제 연결 E2E → T24 웹앱 패키징·품질·CI
후속: T25 지도
```

아래 작업 본문도 이 순서로 배치한다. T01의 실공공 API 확인 두 항목은 키를 확보한 뒤 재개하며, 키 없는 fixture 기반 작업의 실행 순서와 구분한다. T02~T15와 기반·CI 보강 항목은 완료 기록이다.

## 체크 방법

- `[x]`는 구현·실행 검증·main 반영 근거가 있는 완료 항목이다. 계획·fixture만으로 실연동을 완료 처리하지 않는다.
- T번호 하나가 기본 PR 단위다. 의존 작업이 main에 들어온 후 시작한다. 완료 시 해당 항목에 PR·검증 결과 링크를 남긴다.
- 기능 변경은 의도한 assertion의 Red 확인 → 최소 구현 → 대상·전체 테스트 → Green 상태에서 Refactor → 전체 테스트 → 공용 검증 → PR/CI 순서를 따른다. 컴파일·환경·fixture 오류는 기능 Red가 아니다.
- 성공·예외·실패·엣지 케이스를 각 동작에 맞춰 검증한다. 테스트 한글 DisplayName, 명시적 Java 타입, 데이터 준비·격리·teardown, 이름·책임 검토는 [AGENTS.md](AGENTS.md)를 따른다.
- 계획의 Service 파일은 인터페이스이며 `Default{ServiceName}.java` 구현을 같은 작업에 추가한다. Service 테스트는 실제 commit과 별도 조회, MVC/REST Docs는 WebMvcTest와 MockitoBean, 연결된 전체 흐름은 T14의 별도 통합 테스트로 구분한다.
- 아래 경로는 별도 표시가 없으면 새로 만들 파일이다. `src/test/java/com/plugpass/` 하위 테스트와 기능에 필요한 값·응답 타입도 같은 PR에 둔다.
- 업무 HTTP API를 추가할 때는 같은 PR에서 `src/docs/asciidoc/index.adoc`와 REST Docs 테스트를 갱신한다.
- 아래 검증 명령의 테스트명은 생성할 클래스명이다. 필터에 맞는 테스트가 0개이면 통과로 보지 않는다. 공용 검증은 `bash scripts/verify.sh`다.

## 완료한 기반

- [x] B01 — PlugPass 이름·Java 25·Spring Boot 초기 구성. 근거: 초기 커밋 `c59028c`.
- [x] B02 — health 200/UP와 env 404의 실제 HTTP 테스트. 근거: `PlugPassApplicationTests`.
- [x] B03 — H2·JPA 연결과 저장/flush/clear/재조회. 근거: `JpaPersistenceTests`의 테스트 전용 엔티티.
- [x] B04 — REST Docs 계약 검사와 실행 JAR의 문서 제공. 근거: `HealthDocumentationTests`, `scripts/verify-runtime.py`.
- [x] B05 — 객체지향 생활 체조·공식 문서·기능 지도·검증 지침. 근거: [개발 기준](AGENTS.md).
- [x] B06 — 작은 PR·필수 CI·main 보호·자동 squash merge. 근거: [PR #1](https://github.com/seungmin-park/PlugPass/pull/1), [main CI](https://github.com/seungmin-park/PlugPass/actions/runs/36875838604).

## 1주차 — 데이터 계약과 모델

### T01 — 사용할 공공데이터 계약 확인

의존: 없음. 파일: `docs/data-contract.md`, `src/test/resources/publicdata/normal.xml`, `empty.xml`, `unknown-status.xml`.

- [x] 공식 API를 하나 선정하고 공식 URL·신청/인증·페이지 규칙·호출 한도·갱신 특성·이용 조건·확인일을 기록한다.
- [ ] 검증 지역 하나와 충전소/충전기 식별자, 좌표, 커넥터, 이용 제한, 상태 코드, 각 시각 필드의 의미·시간대를 실제 응답으로 확인한다.
- [x] 개인정보·서비스 키가 없는 fixture를 만들고 실제 연동 확인 여부와 미제공 필드를 구분한다. 최신성 `maxAge`와 요청/재시도 예산의 근거를 기록한다.
- [ ] 실응답과 가이드를 대조하고 fixture 구문·필수 식별자·비밀 정보 미포함을 검사한다. 계약 PR과 확인 결과를 연결한 뒤 완료한다.

진행 근거: [PR #4](https://github.com/seungmin-park/PlugPass/pull/4), main `9d93fae`, 필수 CI 성공. [계약](docs/data-contract.md), XML fixture 3개 구문·식별자·건수·키/연락처 미포함 검사 통과. 사용자 선택: 키 없음, 문서/fixture로 진행. 실응답·시간대 확인과 실응답/가이드 대조 항목은 미완료. T02는 plan.md의 승인 지연 예외로 진행한다.

산출: 이후 작업에서 사용할 외부 계약과 재현 입력. 접근 불가이면 원인과 진행 가능한 범위를 남기고 실연동 확인 체크는 열어 둔다.

### T02 — 충전소·충전기 식별과 위치 모델

의존: T01. 파일: `station/domain/Station.java`, `station/domain/Charger.java`, `station/domain/ChargerId.java`, `station/domain/GeoPoint.java`(main 패키지 아래), `station/domain/StationModelTests.java`(test 패키지 아래).

- [x] 다른 공급자·충전소의 같은 충전기 번호가 충돌하지 않는 테스트, 빈 식별자·범위 밖 좌표를 거부하는 테스트를 먼저 작성한다.
- [x] `ChargerId(provider, stationId, chargerId)`와 `GeoPoint(latitude, longitude)`를 정의하고 Station/Charger가 각자의 데이터 유효성을 지키게 한다.
- [x] `./gradlew test --tests '*StationModelTests'`에서 실패 확인 후 구현·통과시키고 PR을 반영한다.

완료 근거: [PR #5](https://github.com/seungmin-park/PlugPass/pull/5), main `11a0766`, [필수 CI 성공](https://github.com/seungmin-park/PlugPass/actions/runs/36881897304). 대상 52건·전체 56건 통과, 공용 검증 종료 0. Red assertion 실패·Green·책임 검토·cmux 표시 불가 범위는 [실행 기록](docs/t02-verification.md)에 남겼다.

산출: 수집·저장의 공통 식별자와 유효한 위치 모델. 외부 API DTO를 업무 객체로 직접 사용하지 않는다.

### T03 — 공급자 상태 코드 정규화

의존: T01, T02. 파일: `station/domain/ChargerStatus.java`, `ingestion/client/ProviderStatusMapper.java`, `ingestion/client/ProviderStatusMapperTests.java`.

- [x] 정상 코드·알 수 없는 코드·null을 입력하는 테스트를 작성한다. 미지원 값은 AVAILABLE이 아니라 UNKNOWN이어야 한다.
- [x] `ProviderStatusMapper.map(String rawCode): ChargerStatus`와 AVAILABLE/OCCUPIED/UNAVAILABLE/UNKNOWN을 구현하고 원본 코드를 보존한다.
- [x] `./gradlew test --tests '*ProviderStatusMapperTests'`의 실패·통과를 확인하고 계약 표와 함께 PR을 반영한다.

완료 근거: [PR #7](https://github.com/seungmin-park/PlugPass/pull/7), 필수 CI 성공·main 반영. 대상 19건·전체 75건·공용 검증 통과. [실행 기록](docs/week1-verification.md).

산출: T01 코드표에 근거한 상태 변환. 운영 시간·접근 제한은 상태 코드 하나로 추정하지 않는다.

### T04 — 중복 없는 업무 데이터 저장

의존: T02, T03. 파일: `station/repository/StationRepository.java`, `station/repository/ChargerRepository.java`, `ingestion/service/StationUpsertService.java`, `ingestion/dto/StationSnapshot.java`, `station/repository/StationPersistenceTests.java`, `ingestion/service/StationUpsertTests.java`. 수정: Station/Charger JPA 매핑.

- [x] Repository 매핑·쿼리는 기본 save 후 조회로 검증한다. 매핑 복원이 목적일 때만 이유를 명시해 flush/clear하며 참조 차이 자체를 assertion하지 않는다.
- [x] 반복 저장·식별자 충돌·역순 응답은 실제 StationUpsertService 트랜잭션 commit 후 별도 조회로 검증한다. 테스트 Transactional 없이 AfterEach에서 생성 데이터만 FK 역순으로 정리한다.
- [x] 유일 제약과 `StationUpsertService.upsert(StationSnapshot snapshot)`을 구현한다. `StationSnapshot`은 T01 필드의 정규화 입력이며 시각의 의미를 보존한다.
- [x] 신뢰할 수 있는 공급자 순서 시각이 있으면 이전 관측의 덮어쓰기를 거부한다. 없으면 순서를 보장할 수 없음을 기록하고 T06의 직렬 수집으로 제한한다.
- [x] `./gradlew test --tests '*StationPersistenceTests' --tests '*StationUpsertTests'`와 전체 테스트·공용 검증을 통과하고 PR을 반영한다.

완료 근거: [PR #8](https://github.com/seungmin-park/PlugPass/pull/8), 필수 CI 성공·main 반영. 대상 22건·전체 97건·공용 검증 통과. 현재 공급자 순서 보장 불가·T06 직렬 제한과 [실행 기록](docs/week1-verification.md)을 함께 남겼다.

산출: 업무용 JPA 모델과 갱신 경계. 기존 PersistenceProbe 테스트를 업무 구현 완료의 근거로 대신하지 않는다.

### T05 — 외부 API 클라이언트

의존: T01, T03, T04의 StationSnapshot 계약. 파일: `ingestion/client/PublicDataClient.java`, `ingestion/dto/StationPage.java`, `ingestion/client/PublicDataClientTests.java`, 외부 페이지 DTO. 수정: `application.properties`의 외부 설정 참조.

- [x] fixture HTTP 서버로 정상·빈 페이지·페이지 마지막·잘못된 본문·인증 오류를 테스트한다. 서비스 키가 로그에 나오지 않는지도 확인한다.
- [x] `PublicDataClient.fetchPage(int page): StationPage`를 구현한다. 페이지 번호 기준과 종료 신호는 T01 계약을 따르고 결과는 StationSnapshot 목록으로 변환한다.
- [x] 연결·응답 timeout과 인증 값의 외부 주입을 적용한다. 공개 수집 실행 API는 추가하지 않는다.
- [x] `./gradlew test --tests '*PublicDataClientTests'`를 통과하고 정상·오류 계약을 문서화한 PR을 반영한다.

완료 근거: [PR #9](https://github.com/seungmin-park/PlugPass/pull/9), main `39ed230`, [필수 CI 성공](https://github.com/seungmin-park/PlugPass/actions/runs/36890384015). 대상 64건·전체 161건·공용 검증 종료 0. [실행 기록](docs/week1-verification.md)에 Red·Green·Refactor와 실패/경계·실연동 한계를 기록했다.

산출: 외부 I/O 경계. 테스트 서버 통과와 실제 공공 API 인증 성공을 구분한다.

## 2주차 — 수집부터 사용자 조회까지

### T06 — 한 번의 수집 실행과 부분 실패 기록

의존: T04, T05. 파일: `ingestion/service/StationSyncService.java`, `ingestion/dto/SyncResult.java`, `ingestion/domain/SyncRun.java`, `ingestion/repository/SyncRunRepository.java`, `ingestion/service/StationSyncTests.java`.

- [x] 여러 페이지 수집, 같은 응답 재실행, 중간 페이지 실패, 잘못된 레코드가 섞인 경우를 테스트한다.
- [x] `StationSyncService.synchronize(): SyncResult`로 페이지를 순차 처리하고 페이지별 짧은 저장 트랜잭션을 사용한다. 네트워크 대기 중 DB 트랜잭션을 열어 두지 않는다.
- [x] 실행 ID·시작/종료·처리/실패 건수·결과를 저장한다. 전체 성공만 lastSuccessfulRunAt을 바꾸고, 부분 실패에서 기존 행을 삭제하지 않는다.
- [x] `./gradlew test --tests '*StationSyncTests'`와 공용 검증으로 저장 결과를 확인하고 PR을 반영한다.

완료 근거: [PR #11](https://github.com/seungmin-park/PlugPass/pull/11), main `e062034`, [필수 CI 성공](https://github.com/seungmin-park/PlugPass/actions/runs/36892084534). 대상18건·전체179건·공용 검증 종료0. [실행 기록](docs/week2-verification.md).

실패 건수의 단위는 실패 페이지 수이며 `failedPageCount`로 저장한다. 처리 수는 입력 레코드 수다. 미수신 페이지의 실패 레코드 수는 추정하지 않는다. 최종 전체258건 검증 근거는 [2주차 최종 검토 기록](docs/week2-verification.md)을 따른다.

산출: 반복 호출해도 중복되지 않는 수집 진입점과 성공/부분 실패/실패를 구분한 SyncResult.

### T07 — 관측 시각에 근거한 최신성 판정

의존: T01, T03, T06. 파일: `freshness/domain/Freshness.java`, `freshness/domain/FreshnessPolicy.java`, `freshness/domain/FreshnessPolicyTests.java`.

- [x] 고정 Clock으로 경과 시간 maxAge 미만·같음·초과, 미래/누락 시각, 재수집한 오래된 응답을 테스트한다.
- [x] `FreshnessPolicy.evaluate(Instant sourceObservedAt, Instant now): Freshness`로 RECENT/STALE/UNVERIFIED를 반환한다. maxAge와 같은 경계는 RECENT다.
- [x] collectedAt이나 상태 변경 시각을 관측 시각 대신 사용하는 경로를 막는다. 신뢰 가능한 관측 시각이 없는 공급자는 UNVERIFIED와 이유를 반환한다.
- [x] `./gradlew test --tests '*FreshnessPolicyTests'`를 통과하고 판정 기준을 설명하는 PR을 반영한다.

완료 근거: [PR #12](https://github.com/seungmin-park/PlugPass/pull/12), main `16083e3`, [필수 CI 성공](https://github.com/seungmin-park/PlugPass/actions/runs/36892966722). 대상14건·전체193건·공용 검증 종료0. [실행 기록](docs/week2-verification.md).

산출: 외부 I/O 없는 최신성 정책. null 관측 시각은 UNVERIFIED이며 미래 시각도 RECENT가 아니다.

### T08 — 주변 충전소 조회 API

의존: T04, T07. 파일: `search/service/StationQueryService.java`, `search/controller/StationController.java`, `search/domain/StationSearchQuery.java`, `search/service/StationSearchTests.java`. 수정: REST Docs 문서.

- [x] 위치·반경 경계 포함, 호환 커넥터, 거리 동률, 빈 결과, 잘못된 좌표/반경/limit의 HTTP 테스트를 작성한다.
- [x] `GET /api/v1/stations`와 `StationQueryService.search(StationSearchQuery query)`를 구현한다. plan.md의 입력 범위·기본 limit·거리/ID 순서를 적용한다.
- [x] 개별 충전기의 상태·최신성을 조합한 요약을 반환한다. 정상 초기 수집 전에는 빈 목록과 데이터 준비 상태를 구분해 제공한다.
- [x] `./gradlew test --tests '*StationSearchTests'`와 REST Docs·공용 검증을 통과하고 PR을 반영한다.

완료 근거: [PR #13](https://github.com/seungmin-park/PlugPass/pull/13), main `828377a`, [필수 CI 성공](https://github.com/seungmin-park/PlugPass/actions/runs/36894771045). 대상49건·전체242건·공용 검증 종료0. [실행 기록](docs/week2-verification.md).

산출: 외부 API를 호출하지 않는 저장 데이터 기반 검색. 지도 프런트엔드는 필요하지 않다.

### T09 — 충전소 상세와 판단 근거

의존: T08. 파일: `search/service/StationDetailTests.java`, 상세 응답 타입. 수정: StationController/StationQueryService와 REST Docs 문서.

- [x] 존재하는 충전소·없는 ID·여러 상태의 충전기·누락된 운영 정보에 대한 테스트를 작성한다.
- [x] `GET /api/v1/stations/{stationId}`에 충전기 상태·원본 코드·관측/수집 시각·최신성·이용 제한·미확인 이유를 제공한다. 없는 ID는 404다.
- [x] `./gradlew test --tests '*StationDetailTests'`와 API 문서·공용 검증을 통과하고 PR을 반영한다.

완료 근거: [PR #15](https://github.com/seungmin-park/PlugPass/pull/15), main `3f354d6`, [필수 CI 성공](https://github.com/seungmin-park/PlugPass/actions/runs/36897055110). 대상14건·전체257건·공용 검증 종료0. 실제 fixture HTTP→H2→검색/상세와 공급자503 중 기존 조회 확인. [실행 기록](docs/week2-verification.md).

산출: “왜 이 충전소를 믿거나 다시 확인해야 하는가”를 보여 주는 조회 응답.

## 3주차 — 대체 후보와 안정적인 반복 수집

### T10 — 근거가 있는 대체 후보

의존: T08, T09. 파일: `recommendation/domain/CandidatePolicy.java`, `recommendation/domain/StationCandidate.java`, `recommendation/domain/CandidateGroups.java`, `recommendation/controller/RecommendationController.java`, `recommendation/domain/RecommendationTests.java`. 수정: REST Docs 문서.

- [x] 호환되지 않는 커넥터, 명시적 이용 제한, stale/unknown, 제외한 충전소, 후보 0개, 거리 동률을 테스트한다.
- [x] `CandidatePolicy.rank(List<StationCandidate> candidates): CandidateGroups`와 `GET /api/v1/recommendations`를 구현한다. StationSearchQuery로 검색한 뒤 제외 ID를 빼고, 추천 판단에 필요한 값만 StationCandidate로 전달한다.
- [x] RECENT·AVAILABLE 후보와 UNVERIFIED인 확인 필요 후보를 분리하고 reasonCodes를 제공한다. 미확인 운영 조건은 경고로 표시하며 STALE을 확정 이용 가능으로 올리지 않는다.
- [x] `./gradlew test --tests '*RecommendationTests'`와 REST Docs·공용 검증을 통과하고 PR을 반영한다.

완료 근거: [PR #19](https://github.com/seungmin-park/PlugPass/pull/19), main `6c40b0b`, [필수 CI 성공](https://github.com/seungmin-park/PlugPass/actions/runs/37220612868). 추천 관련27건·전체287건·공용 검증 종료0. fixture 수집→H2→추천 실제 HTTP, 그룹별 limit 전 판단·제외와 REST Docs를 확인했다. [Red/Green·책임·실패/경계 기록](docs/week3-verification.md).

산출: plan.md의 결정 가능한 규칙에 따른 후보. 예측 정확도나 충전 성공률을 임의로 표시하지 않는다.

### T11 — 중복 실행을 막는 수집 스케줄

의존: T06. 파일: `ingestion/scheduler/SyncScheduler.java`, `ingestion/scheduler/SyncSchedulerTests.java`. 수정: 수집 활성화·간격 설정.

- [x] 아직 실행 중인 수집을 다시 호출할 때 중복되지 않고, 예외 종료 후 다음 실행이 가능한지 테스트한다.
- [x] 단일 인스턴스용 실행 소유권을 두고 고정 지연 방식으로 synchronize를 호출한다. 수집은 기본 비활성화하며 명시적 설정과 인증이 있을 때 켠다.
- [x] T01의 호출 한도와 한 회차의 페이지 수로 간격을 정한다. `./gradlew test --tests '*SyncSchedulerTests'`에서 실제 장시간 sleep 대신 제어 가능한 실행기를 사용하고 PR을 반영한다.

완료 근거: [PR #20](https://github.com/seungmin-park/PlugPass/pull/20), main `8607e46`, [필수 CI 성공](https://github.com/seungmin-park/PlugPass/actions/runs/37220900851). 대상6건·전체293건·공용 검증 종료0. 중복 즉시 SKIPPED, 예외 후 회복, 기본 비활성화·인증/간격 방어를 확인했다. 30분 고정 지연과 회차 최대20요청은 예정 수집 하루 최대960요청이며 실제 계정 quota·수동 실행은 별도 확인한다. [실행 기록](docs/week3-verification.md).

산출: 한 프로세스에서 중복되지 않는 반복 수집. 분산 락·다중 인스턴스 보장은 하지 않는다.

### T12 — 외부 장애의 시간·재시도 제한과 복구

의존: T05, T06, T11. 파일: `ingestion/service/IngestionRecoveryTests.java`. 수정: PublicDataClient/StationSyncService의 실패 분류와 실행 예산.

- [x] timeout, 429, 5xx, 401/403, 깨진 본문, 페이지 무진행을 주입한다. 종료까지 요청 횟수·총 소요 예산에 상한이 있는지 확인한다.
- [x] 일시 오류만 제한 재시도하고 인증·계약 오류는 같은 요청을 반복하지 않는다. Retry-After는 남은 실행 예산 안에서 적용한다.
- [x] 장애 중 조회가 기존 데이터를 반환하되 최신성은 시간에 따라 낮아지는지, 공급자 복구 후 다음 수집이 갱신하는지 테스트한다.
- [x] `./gradlew test --tests '*IngestionRecoveryTests'`와 공용 검증을 통과하고 실패 분류·복구 설명을 포함한 PR을 반영한다.

완료 근거: [PR #22](https://github.com/seungmin-park/PlugPass/pull/22), main `c1e4cd2`, [필수 CI 성공](https://github.com/seungmin-park/PlugPass/actions/runs/37222055266). 대상31건·전체324건·공용 검증 종료0. 제한 재시도·Retry-After·페이지/요청/시간 상한·interrupt·부분 commit 보존·STALE 경과·다음 회차 회복을 검증했다. 기존 인증/깨진 응답 HTTP 검사도 유지했다. [실행 기록](docs/week3-verification.md).

산출: 무한 재시도 없이 끝나고 다음 실행에서 회복하는 수집. 프로세스 재시작 뒤 H2 데이터 복구와는 구분한다.

### T13 — 수집 상태와 데이터 품질 관측

의존: T06, T11, T12. 파일: `ingestion/metrics/IngestionMetrics.java`, `ingestion/metrics/IngestionMetricsTests.java`, `docs/operations.md`.

- [x] 성공/부분 실패/실패, 재시도, 마지막 성공 이후 경과, 조회 데이터의 최신성 분포를 구분하는 검증을 작성한다.
- [x] 실행 ID·처리 건수·실패 원인을 로그와 지표로 연결한다. 비밀 키·응답 원문 전체를 로그에 남기지 않고 충전소 ID를 지표 태그로 사용하지 않는다.
- [x] `./gradlew test --tests '*IngestionMetricsTests'`를 통과하고 health와 데이터 최신성의 차이·운영 확인 명령을 문서화한 PR을 반영한다.

완료 근거: [PR #25](https://github.com/seungmin-park/PlugPass/pull/25), main `e8366d0`, [필수 CI 성공](https://github.com/seungmin-park/PlugPass/actions/runs/37222985184). 대상6건, main 구조 검사 통합 후 Java353건·Python8건 실패/오류/skip0·공용 검증 종료0. 실제 품질/경과 metrics HTTP와 비밀값 없는 종료 로그, 실행 JAR 기본 metrics404·추천200·문서 일치를 확인했다. [운영 확인](docs/operations.md), [실행 기록](docs/week3-verification.md).

산출: 서버 정상과 데이터 수집 정상의 차이를 관찰하는 방법. 외부 모니터링 서비스 전송은 이 작업의 필수 범위가 아니다.

## 4주차 — 연결된 흐름과 성능 기준선

### T14 — 수집부터 대체 후보까지 실제 HTTP 검증

의존: T07~T13. 파일: `src/test/java/com/plugpass/ChargingJourneyTests.java`, `docs/demo.md`. 수정: `scripts/verify-runtime.py`, 기능 지도·검증 스킬.

- [x] 현재 cmux workspace·surface와 전용 pane을 확인해 실행 명령·로그·실제 요청을 보이게 준비한다. 연결 불가 시 원인과 대체 범위를 기록한다.
- [x] 제어 가능한 공급자 응답을 수집한 후 실제 HTTP로 주변 조회 → 상세 → 첫 충전소 제외 → 대체 후보 조회를 수행하고 값까지 assertion한다.
- [x] 같은 데이터 재수집, 시간 경과 후 stale, 외부 장애, 복구 후 갱신, 후보 없음도 확인한다. 실제 공공 API 표본 검증은 별도 증거로 남긴다.
- [x] `./gradlew test --tests '*ChargingJourneyTests'`와 업데이트된 공용 검증을 통과하고 결과·한계를 포함한 PR을 반영한다.

산출: 직접 재현할 수 있는 핵심 흐름. fixture 공급자와 실제 공급자 검증을 서로 대신하지 않는다.

검증·반영: [PR #28](https://github.com/seungmin-park/PlugPass/pull/28), 필수 CI SUCCESS·main ed855d45. Java356·Python11 실패/오류/skip0. [종합 근거](docs/week4-verification.md)·[데모](docs/demo.md). cmux 화면·실공공 인증은 별도 미확인.

### T15 — 조회 성능 기준선 측정

의존: T08, T10, T14. 파일: `scripts/performance/`의 선택한 부하 도구 실행 파일, `docs/performance.md`.

- [x] T14의 응답 정확성 assertion을 유지하면서 생성 데이터·시드·실행 환경·조회 분포를 고정하고 부하 도구 하나를 선택한다.
- [x] plan.md의 10,000개 충전소·50,000개 충전기·동시 사용자 20명·30초 준비·3분 측정 조건을 실행 가능한 명령으로 만든다.
- [x] p50/p95·예상하지 않은 오류율·조회별 SQL 수·병목과 목표(p95 300ms, 오류율 1% 미만) 달성 여부를 기록한다. 생성 데이터 결과임을 표시한다.
- [x] 명령·원본 결과 위치·대상 SHA를 포함한 측정 PR을 반영한다. 측정 전후 데이터 정확성도 확인한다.

산출: 개선 여부를 판단할 재현 가능한 기준선. 숫자를 측정 없이 완료로 채우지 않는다.

검증·반영: [PR #29](https://github.com/seungmin-park/PlugPass/pull/29), 필수 CI SUCCESS·main a7793423. Java357·Python14 실패/오류/skip0. 실제 10,000/50,000·20명·30초/180초,4591요청·오류0%·p50 768.01ms·p95 1425.41ms. **300ms 목표 미달**이며 측정 완료를 성능 목표 달성으로 바꾸지 않는다. [성능 원본·재현](docs/performance.md)·[검증/전달 근거](docs/week4-verification.md).

## 백엔드 조회 개선·장애 검증 (미착수)

### T16 — 측정된 조회 병목 하나 개선

의존: T15. 수정 후보: StationRepository/StationQueryService 및 조회 테스트, `docs/performance.md`.

- [x] T15 결과로 N+1·불필요한 전체 조회·정렬 비용 등 실제 병목 하나를 선택한다. 목표를 이미 만족하고 개선 근거가 없으면 생략 사유를 T15 기록에 남긴다.
- [x] 병목을 재현하는 SQL 수·결과 동등성 검증을 먼저 작성하고 필요한 쿼리·인덱스·조회 범위만 개선한다. 캐시를 기본 해결책으로 넣지 않는다.
- [x] 같은 조건으로 전후 성능과 조회 결과 일치를 확인하고 공용 검증·CI를 통과한 PR을 반영한다. 개선이 없으면 성공으로 기록하지 않는다.

산출: 근거 있는 개선 한 건 또는 개선 불필요 판단. 생략은 `[x]` 옆에 “측정 결과로 생략”과 근거를 명시한다.

완료 근거: [PR #33](https://github.com/seungmin-park/PlugPass/pull/33), main `607c1c8`, [필수 CI](https://github.com/seungmin-park/PlugPass/actions/runs/37229800029) 성공. Java360건·Python14건·공용 검증 종료0. 동일20명/180초 전체p95 1425.41→75.04ms, 오류0·응답/DB건수 동일. [T16 기록](docs/t16-verification.md).

### T17 — 장애·복구 시나리오 종합 확인

의존: T12, T14, T15, T16의 판단. 파일: `docs/reliability.md`. 필요 시 기존 장애/HTTP 테스트와 검증 스크립트 수정.

- [x] 공급자 지속 장애·중간 페이지 실패·반복 응답·복구 응답·동시 스케줄 호출을 기존 테스트로 재현하고 누락된 assertion을 추가한다.
- [x] 재시도 상한, 기존 정보 보존, stale/미확인 표시, 마지막 성공 시각의 정확성, 복구 뒤 중복 없는 갱신을 확인한다.
- [x] 서버 재시작 후 메모리 H2가 비워지는 동작과 재수집 전 데이터 준비 상태를 확인한다. 이를 영속 복구 성공으로 보고하지 않는다.
- [x] 실제 종료 코드·assertion·로그·한계를 기록하고 관련 공용 검증을 통과한 PR을 반영한다.

산출: 어떤 실패에서 어떻게 동작하는지 재현 가능한 복구 기록.

완료 근거: [PR #34](https://github.com/seungmin-park/PlugPass/pull/34), main `5eca56e`, [필수 CI](https://github.com/seungmin-park/PlugPass/actions/runs/37230460327) 성공. 대상35건·전체Java360건·Python16건·공용검증종료0. 별도JVM3회 실제HTTP로 수집완료→H2소실/준비전→재수집완료. [장애/복구 기록](docs/reliability.md).

## 프런트 — 목록 중심 웹앱

범위·화면·API·실패 조건은 [프런트 계획](docs/frontend-plan.md)을 따른다. 아래 프런트의 `src/...`·설정 파일은 `frontend/` 기준이며, `frontend/`·`src/test/`·`scripts/`로 표시한 경로는 저장소 기준이다. T18~T20의 실제 구현·검증은 체크와 근거를 따르며 T21 이후는 계획이다.

### T18 — 프런트 실행과 검증 기반

의존: T16, T17. 파일: `index.html`, `package.json`, `package-lock.json`, `vite.config.ts`, `vitest.config.ts`, `tsconfig*.json`, `eslint.config.js`, `tests/setup.ts`, `tests/vueWarnings.ts`, `tests/vueWarnings.spec.ts`, `src/main.ts`, `src/App.vue`, `src/router/index.ts`, `src/styles/base.css`, `src/features/search/components/{SearchForm.vue,SearchForm.spec.ts}`, `src/features/search/views/StationSearchView.vue`. 수정: `.gitignore`, 공용 verify·CI, 기술 기록·출처 목록.

- [x] 공식 Vue 생성 구성을 확인하고 Vue 3·TypeScript·Vite와 상태/통신/테스트 도구의 실제 호환 버전·Node를 lockfile과 프로젝트 설정에 고정한다.
- [x] [엄격한 검증 기준](docs/frontend-plan.md#101-엄격한-정적-검사와-vue-경고)에 따라 strict·배열/null·optional·템플릿 검사를 켜고 운영/테스트/설정/E2E TS의 검사 범위를 연결한다. `build`는 type-check 성공 후 Vite를 실행하고 lint는 `--max-warnings 0`을 적용한다. props 타입·템플릿 참조·null/배열 접근·undefined를 허용하지 않은 optional 속성 대입·테스트/설정 타입·lint 경고의 임시 위반을 각각 실제 거부하고 복원한다.
- [x] Vue 경고 수집기의 0건 통과·진단 발생 시 실패·테스트별 격리를 먼저 테스트한다. setup에 연결한 뒤 실제 Vue 경고·console 진단·비동기 예외의 임시 probe에서 runner 비정상 종료를 확인하고 복원한다. 빈 handler·console mock·오류 무시로 수집을 우회하지 못하게 한다.
- [x] runner·DOM 환경을 준비한 뒤 반경·커넥터 라벨과 검색 버튼, 자동 위치 권한 요청 없음의 테스트를 먼저 작성한다. 최소 빈 컴포넌트에서 누락된 화면의 assertion 실패를 확인하고 검색 진입점을 구현한다.
- [x] `/app/index.html`·Vite base `/app/`·hash router를 구성하고 `/`에서 `/stations`로 이동한다. 기본 반경은 1000m, 커넥터는 DC_COMBO다.
- [x] `npm --prefix frontend run test:unit -- tests/vueWarnings.spec.ts src/features/search/components/SearchForm.spec.ts`와 프런트 전체 test:unit·type-check·lint·build를 기존 공용 verify·필수 CI에 연결하고 실제 실패 전파·복원 후 통과와 PR 반영을 확인한다. 설치 버전과 실제 Red/Green을 기록한다.

산출: 브라우저에서 열리는 검색 진입점과 실행 가능한 프런트 검증 명령. scaffolding 통과를 검색 사용자 흐름 완료로 보고하지 않는다.

T18 전달: PR [#36](https://github.com/seungmin-park/PlugPass/pull/36) 실제 머지 `1e4e544`, 필수 CI [37231741154](https://github.com/seungmin-park/PlugPass/actions/runs/37231741154) 성공. Java360·Python21·프런트9, strict/진단 probe17건 거부·복원.

### T19 — URL 조건과 API 계약

의존: T18. 파일: `src/features/search/{types.ts,criteria.ts,api/stationApi.ts}`, `src/features/station-detail/{types.ts,api/stationDetailApi.ts}`, `src/features/recommendation/{types.ts,api/recommendationApi.ts}`, `src/features/charging-info/types.ts`, `src/shared/api/{httpClient.ts,apiError.ts}`와 각 `.spec.ts`.

- [x] 조건 없음·부분 누락·중복 query·위경도/반경/limit 경계·비정수·미지원 커넥터·안전 정수 밖 ID를 구분하는 입력 테스트를 먼저 작성한다.
- [x] `parseSearchCriteria(query)`와 `getStations(criteria, signal?)`, `getStationDetail(stationId, signal?)`, `getRecommendations(criteria, excludeStationId, signal?)`를 현재 Java DTO·REST Docs 계약에 맞춘다.
- [x] 정확한 HTTP 경로·query·취소 signal, timeout 10000ms·400 fields·404·429·5xx·network·취소·잘못된 JSON/응답 구조를 경계 테스트로 확인한다. 자동 재시도는 하지 않는다.
- [x] criteria·각 API·apiError 대상 테스트에서 assertion 실패→최소 구현→통과를 확인하고 전체 테스트·공용 검증·PR을 반영한다.

산출: 화면이 공유하는 검색 조건·응답 타입·HTTP 오류 계약. nullable 시각은 보존하고 추천에 없는 좌표·dataReady를 만들어내지 않는다.

완료 근거: [PR #37](https://github.com/seungmin-park/PlugPass/pull/37), main `3191b89`, [필수 CI](https://github.com/seungmin-park/PlugPass/actions/runs/37232622126) 성공. Java360·Python21·프런트142·타입/lint/build·실제HTTP·재시작3회 성공, 실패/오류/skip0. Red107건과 필수값 Red6건·최소 구현·책임 리뷰는 [실행 기록](docs/frontend-verification.md), [원격 결과](docs/evidence/t19/ci-summary.json).

### T19 범위 디자인 반영 — 사용자 제공 Stitch 시안

사용자 확정: 현재 검색 화면과 공통 디자인에 적용하며 후속 기능으로 확장하지 않는다. [시안 대응·책임·검증](docs/frontend-design.md)을 따른다.

- [x] 첨부 검색 시안·로고·색·입력 pill을 Vue 현재 화면에 반영하고 10km·5개 커넥터·submit 계약을 유지한다.
- [x] native radio·본문 건너뛰기 assertion Red6건 후 Green147건을 확인한다. 스타일 자체의 복제 테스트는 추가하지 않는다.
- [x] 현재 Codex의 실제 선택·버튼·방향키·Tab/Enter·없는 경로 복귀와 1440/375/320px 화면을 확인한다.
- [x] 최종 공용 검증과 화면 assertion의 실제 결과·범위·한계를 [실행 기록](docs/frontend-verification.md)에 남긴다. 서명 PR·필수 CI·main 실제 반영은 [전달 절차](docs/delivery-workflow.md)로 확인하며 해당 PR의 원격 상태를 완료 기준으로 삼는다.

### T20 — 위치 선택과 주변 검색 화면

의존: T19. 파일: `src/shared/location/useCurrentLocation.ts`, `src/shared/ui/RequestState.vue`, `src/features/search/{stores/stationSearchStore.ts,components/StationCard.vue}`, `src/features/charging-info/{presentation.ts,components/StatusBadge.vue,components/FreshnessBadge.vue}`와 각 `.spec.ts`. 수정: SearchForm·StationSearchView·router.

- [x] 위치 성공·권한 거부·10초 제한·늦은 위치 응답, URL 조건 복원·뒤로 가기, 데이터 준비 전/부분 결과/빈 결과, 400·서버 오류·응답 역전의 테스트를 먼저 작성한다.
- [x] `useCurrentLocation().requestLocation()`과 `useStationSearchStore().search(criteria)`를 구현한다. 폼은 입력, URL은 확정 조건, Store는 조회 상태·결과·요청 순번을 소유한다.
- [x] 위치 버튼 또는 명시적인 검증용 예시 위치 37.5/127에서 검색한다. 직선거리·이용 가능 보고 수·상태/최신성 배지·표시 상한을 보여 주고 이전 조건의 결과가 새 조건을 덮지 않게 한다.
- [x] `npm --prefix frontend run test:unit -- src/features/search src/shared/location src/features/charging-info`에서 실패·통과와 전체 검증을 확인하고 PR을 반영한다.

산출: 실제 API로 주변 충전소를 검색하는 화면. 데이터 준비 전·빈 결과·통신 오류를 구분하며 최신성 판정은 서버에 둔다.

구현·로컬 검증 근거: [T20 실행 기록](docs/t20-verification.md). 프런트210건/17파일·Java360건·Python21건·타입/lint/build·실제HTTP·재시작3회 통과. 현재 cmux의 실제 위치 권한 거부→예시 위치→H2 검색·빈 결과·뒤로 가기·직접 링크·375/320px를 확인했다. 전달 완료는 이 변경 PR의 필수 CI와 실제 머지 상태를 따른다.

### T21 — 충전소 상세와 정보 근거

의존: T20. 파일: `src/features/station-detail/{stores/stationDetailStore.ts,views/StationDetailView.vue,components/ChargerList.vue}`와 각 `.spec.ts`. 수정: 공용 charging-info 표시/테스트, router·검색 카드 이동.

- [x] 상세 목록·빈 충전기·없는 ID·관측 시각 null·이용 조건 누락·알 수 없는 상태/사유·시각 변환·빠른 ID 변경의 응답 역전 테스트를 먼저 작성한다.
- [x] `useStationDetailStore().loadStation(stationId)`와 상태·최신성·사유·시각 표시를 구현한다. 관측 시각·수집 시각을 구분하고 AVAILABLE+UNVERIFIED를 확정 이용 가능으로 표시하지 않는다.
- [x] 검색 조건을 보존해 목록/대체 후보로 이동한다. 조건 없는 상세 직접 링크는 상세를 조회하되 대체 후보 조회 전에 조건을 선택하게 한다. 원본 비고는 텍스트로 표시한다.
- [x] `npm --prefix frontend run test:unit -- src/features/station-detail src/features/charging-info`에서 실패·통과와 전체 검증을 확인하고 PR을 반영한다.

산출: 충전기 상태와 정보의 근거를 확인하는 상세 화면. 관측 시각 누락을 수집 시각으로 대체하거나 추천 규칙을 프런트에서 재계산하지 않는다.

구현·로컬 검증 근거: [T21 실행 기록](docs/t21-verification.md). 상세/표시55건·프런트229건/20파일·Java360·Python21·공용verify종료0. 현재cmux 실제 검색→상세→목록조건 유지·실제404·375px를 확인했다. 완료: [PR #42](https://github.com/seungmin-park/PlugPass/pull/42), main27c0f79, [필수CI성공](https://github.com/seungmin-park/PlugPass/actions/runs/37284364588).

### T22 — 같은 조건의 대체 후보 화면

의존: T21. 파일: `src/features/recommendation/{stores/recommendationStore.ts,views/AlternativeStationsView.vue,components/CandidateGroup.vue}`와 각 `.spec.ts`. 수정: 상세의 대체 후보 이동·router.

- [x] 같은 검색 조건·현재 충전소 제외 ID, 세 후보 그룹·빈 결과·알 수 없는 사유·잘못된 직접 링크·독립 재시도·응답 역전 테스트를 먼저 작성한다.
- [x] `useRecommendationStore().loadAlternatives(criteria, excludeStationId)`를 구현하고 preferred/requiresConfirmation/excluded의 그룹·순서·사유를 그대로 표시한다.
- [x] 같은 조건의 검색 메타를 조회해 준비 상태를 알리되, 메타 조회만 실패해도 성공한 후보를 유지한다. 후보를 누르면 실제 상세를 조회한다.
- [x] `npm --prefix frontend run test:unit -- src/features/recommendation`에서 실패·통과와 전체 검증을 확인하고 PR을 반영한다.

산출: 현재 충전소를 제외한 대체 후보와 확인/제외 이유. 응답에 없는 좌표·충전기 수·전체 건수를 추측하지 않는다.

구현·로컬 검증: [T22 실행 기록](docs/t22-verification.md). 추천/진단기38건·전체프런트262건/24파일·기존Java360/Python21. 현재cmux 실제 검색→상세→제외ID후보→다른실제상세·375px 확인. 완료: [PR #43](https://github.com/seungmin-park/PlugPass/pull/43), main7ca266a, [필수CI성공](https://github.com/seungmin-park/PlugPass/actions/runs/37286517636).

### T23 — 브라우저와 실제 백엔드 연결 검증

의존: T22. 파일: `src/test/java/e2e/{ChargingDemoApplication.java,DemoProvider.java,ChargingDemoApplicationTests.java}`, `src/test/resources/publicdata/browser-demo.xml`, `frontend/playwright.config.ts`, `frontend/e2e/{fixtures.ts,charging-journey.spec.ts,ui-failures.spec.ts}`, `scripts/frontend-e2e.sh`, `docs/frontend-verification.md`. 수정: build.gradle의 test-classpath 실행 경로·프런트 명령·공용 JAR 검사·필수 Java suite 목록.

- [x] 합성 충전소 2곳의 fixture HTTP→실제 수집→H2→검색/상세/제외 추천을 테스트한다. 공급자·시간만 제어하고 실제 Controller/Service/Repository를 사용한다.
- [x] 브라우저에서 예시 위치→검색→첫 상세→대체 후보→다른 상세의 텍스트·ID·제외 조건을 assertion한다. 권한 거부·빈 결과·HTTP 오류·timeout·응답 역전은 별도 UI 실패 spec으로 검증한다.
- [x] 개발 모드에서 공용 E2E fixture의 최초 이동 전 console/pageerror 감시를 모든 spec에 적용한다. 초기 mount Vue 경고·클릭 후 비동기 경고·처리되지 않은 예외를 각각 임시 probe로 발생시켜 Playwright 실패를 확인하고 복원한다. HTTP 실패의 예상 진단은 해당 테스트에서 종류·메시지/URL·횟수로 검증하고 Vue 경고의 전역 허용을 금지한다.
- [x] 현재 cmux의 확인한 workspace·보조 pane에 서버/러너 로그를 표시하고 headed/UI assertion과 같은 workspace의 실제 브라우저 클릭을 진행한다. mock UI 범위와 실제 DB 연결 범위를 구분한다.
- [x] 종료 코드·assertion·HTML/trace·서버 로그, 운영 JAR의 e2e 클래스 부재를 확인하고 공용 검증·PR을 반영한다. 직접 시작한 서버만 종료하고 검증 pane은 유지한다.

산출: 실제 검색→상세→대체 후보 연결과 실패 화면의 실행 근거. 기존 흐름의 첫 통과는 기존 동작 확인이며 기능 Red로 바꾸어 보고하지 않는다. cmux 불가·실공공 API 미확인은 별도로 기록한다.

완료: [T23 실행기록](docs/t23-verification.md), [PR #44](https://github.com/seungmin-park/PlugPass/pull/44), main5c31b58, [필수CI성공](https://github.com/seungmin-park/PlugPass/actions/runs/37289909177).

### T24 — 웹앱 패키징과 품질 검사

의존: T23. 수정: `build.gradle`, `frontend/src/styles/base.css`, View/Component·관련 테스트, `frontend/eslint.config.js`, `scripts/verify.sh`, `scripts/verify-runtime.py`·검사기 회귀 테스트, `.github/workflows/verify.yml`, 기능 지도·검증 스킬·실행 기록.

- [ ] 모바일 375×812·데스크톱 1440×900, 키보드 조작·라벨·오류 focus·상태 알림의 테스트를 먼저 작성하고 필요한 화면을 수정한다.
- [ ] 실제 JAR의 `/app/index.html`·JS/CSS·직접 링크·같은 origin API·기존 REST Docs를 검증한다. 누락 assertion의 실패를 확인한 뒤 프런트 build→bootJar의 `static/app/` 포함을 구현한다.
- [ ] Vue→Axios 직접 의존과 API→Store/View 역의존, 필수 suite 누락·0개·skip·failure·error를 기존 lint/검사기로 거부한다. 검사기 회귀 테스트와 실제 임시 위반의 거부·복원을 확인한다.
- [ ] T18의 필수 타입·lint·단위 테스트를 유지하며 개발 모드 경고 E2E와 배포용 JAR 웹앱 검증까지 기존 공용 verify·필수 `PlugPass verify`에 연결한다. 임시 Vue 경고·실행 예외·필수 E2E 누락/0개/skip이 실제 실패로 전파되는지 확인하고 복원 후 전체 검증한다. 실제 결과·책임/이름·공개 계약을 리뷰하고 사용법·기능 지도·검증 기록을 해당 PR에서 갱신한다.

산출: Spring JAR에서 제공되는 웹앱과 필수 품질 검사. 개발 서버 검증·JAR HTTP·원격 CI·화면 E2E의 결과를 구분한다.

구현·로컬 검증: [T24 실행기록](docs/t24-verification.md). Java361·프런트262·Python32·개발E2E17·JAR브라우저2건. 실제main 반영·필수CI 확인 뒤 완료체크를 기록한다.

## 후속 — 지도와 목록 연결

### T25 — 지도와 목록 선택 연결

의존: T24, 지도 범위 결정. 파일·SDK는 [후속 지도 계획](docs/frontend-plan.md)의 범위를 확정할 때 정한다.

- [ ] SDK 공식 조건·키/허용 도메인·출처 표시·지원 브라우저와 지도에 표시할 데이터 범위를 확인한다. 추천 응답의 좌표 부재를 고려한다.
- [ ] 마커/목록 선택·조건 변경·SDK 실패 테스트를 먼저 작성하고 지도와 카드 선택을 연결한다. SDK 실패 시 목록은 유지한다.
- [ ] 대상·전체 검증과 현재 cmux에서 실제 지도 흐름을 확인하고 관련 계획·검증 근거를 갱신한 PR을 반영한다.

산출: 첫 목록 버전 이후의 지도 탐색. 주소 검색·경로 안내·유료 실행·공개 배포를 자동으로 포함하지 않는다.

## 작업마다 공통으로 확인할 완료 조건

### CI 보강 — 구조·핵심 테스트 누락 검사 (2026-10-05)

- [x] 도메인 규칙의 정확성은 기존 동작 테스트에 두고, 의존성·공개 변경 경로의 형태는 ArchUnit 검사와 책임 리뷰로 분리한다.
- [x] Controller·도메인·엔티티·Service 계약·Service/MVC 테스트의 허용/금지 구조로 실제 Red→Green을 확인한다.
- [x] 공용 verify에 Python 검사기 회귀 테스트와 필수 suite XML 비교를 연결한다. 고정 총건수·coverage 비율을 강제하지 않는다.
- [x] 실제 Controller의 임시 금지 의존성과 필수 XML 누락을 거부하고 원본을 복원한다.
- [x] 사용자 선택에 따라 JaCoCo를 제외한다. 기존 기능의 실행 가능한 동작 테스트를 유지한다.

자동 검사의 정확한 선택 범위·리뷰 한계·명령·결과는 [CI 보강 기록](docs/ci-boundaries-verification.md)에 남긴다. 원격 CI와 머지 상태는 이 변경 PR에서 별도로 확인한다.

### 공통 완료 템플릿

전역 agent-engineering의 현재 프로젝트 적용 범위와 실제 결과는 [적용 기록](docs/agent-engineering-application.md)에 연결한다. 기능별 진행 상태는 바꾸지 않으며, 도구 테스트·지침 검토·에이전트 행동 평가를 구분한다.

- [ ] 이번 PR이 해결하는 동작 하나와 의존 작업을 설명했다.
- [ ] 구현 변경은 의도한 assertion 실패로 Red를 확인했고 Green·Refactor 후 대상·전체 테스트를 실행했다.
- [ ] 성공·예외·실패·엣지 케이스와 이름·책임·공개 계약 검토 결과를 기록했다.
- [ ] 필요한 REST Docs·기능 지도·검증 절차를 함께 갱신했다.
- [ ] 로컬 E2E의 cmux 표시 여부와 실제 검증 범위를 기록했다.
- [ ] 현재 PR head의 필수 CI를 확인하고 자동 머지 완료를 확인했다.
- [ ] 검증한 소스와 현재 소스·PR head의 일치를 확인했고 실패·누락·skip·머지 차단을 구분했다.
- [ ] 관련 T항목에 PR·검증 근거를 연결했다.

이 마지막 목록은 매 작업에 복사해서 사용하는 템플릿이며 전체 프로젝트의 진행률 집계에는 포함하지 않는다.
