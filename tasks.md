# PlugPass 작업 체크리스트

**목표·설계:** [plan.md](plan.md). T01 문서 계약은 main 반영, 인증된 실연동은 키 없어 미확인. T02~T15는 구현·검증·main 반영 완료. 4주차 체크8개를 완료했고 조회 p95 목표 미달을 기록했다. T01 실응답 확인 두 항목은 키 없어 보류한다. 아래 하위 체크와 근거를 따른다.

**실행 방식:** 작업 하나씩 순서대로 진행한다. 구현 시 `superpowers:executing-plans`를 사용한다. 이 파일을 만들었다는 이유로 이후 작업 전체를 자동 실행하지 않는다.

**공통 제약:** Java 25·Spring Boot 4.1.1·JPA/H2·Spring REST Docs, 단일 인스턴스·공급자 1곳·검증 지역 1곳. 공식 계약을 먼저 확인한다. 현재 H2는 메모리 모드다. 작은 PR·필수 CI·자동 머지와 cmux 검증 정책을 유지한다.

**프런트 추가 (2026-10-05):** 사용자는 목록 중심 반응형 웹앱으로 검색·상세·대체 후보를 먼저 완성하고 지도를 후속 단계로 선택했다. [프런트 계획](docs/frontend-plan.md)에 따라 T16→T17→F01~F07→T18 순으로 진행한다. 프런트 작업은 모두 미착수이며 새 명령·테스트·의존성도 아직 없다.

## 체크 방법

- `[x]`는 구현·실행 검증·main 반영 근거가 있는 완료 항목이다. 계획·fixture만으로 실연동을 완료 처리하지 않는다.
- T/F번호 하나가 기본 PR 단위다. 의존 작업이 main에 들어온 후 시작한다. 완료 시 해당 항목에 PR·검증 결과 링크를 남긴다.
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

## 5~6주차 — 개선·복구·포트폴리오 완성

### T16 — 측정된 조회 병목 하나 개선

의존: T15. 수정 후보: StationRepository/StationQueryService 및 조회 테스트, `docs/performance.md`.

- [ ] T15 결과로 N+1·불필요한 전체 조회·정렬 비용 등 실제 병목 하나를 선택한다. 목표를 이미 만족하고 개선 근거가 없으면 생략 사유를 T15 기록에 남긴다.
- [ ] 병목을 재현하는 SQL 수·결과 동등성 검증을 먼저 작성하고 필요한 쿼리·인덱스·조회 범위만 개선한다. 캐시를 기본 해결책으로 넣지 않는다.
- [ ] 같은 조건으로 전후 성능과 조회 결과 일치를 확인하고 공용 검증·CI를 통과한 PR을 반영한다. 개선이 없으면 성공으로 기록하지 않는다.

산출: 근거 있는 개선 한 건 또는 개선 불필요 판단. 생략은 `[x]` 옆에 “측정 결과로 생략”과 근거를 명시한다.

### T17 — 장애·복구 시나리오 종합 확인

의존: T12, T14, T15, T16의 판단. 파일: `docs/reliability.md`. 필요 시 기존 장애/HTTP 테스트와 검증 스크립트 수정.

- [ ] 공급자 지속 장애·중간 페이지 실패·반복 응답·복구 응답·동시 스케줄 호출을 기존 테스트로 재현하고 누락된 assertion을 추가한다.
- [ ] 재시도 상한, 기존 정보 보존, stale/미확인 표시, 마지막 성공 시각의 정확성, 복구 뒤 중복 없는 갱신을 확인한다.
- [ ] 서버 재시작 후 메모리 H2가 비워지는 동작과 재수집 전 데이터 준비 상태를 확인한다. 이를 영속 복구 성공으로 보고하지 않는다.
- [ ] 실제 종료 코드·assertion·로그·한계를 기록하고 관련 공용 검증을 통과한 PR을 반영한다.

산출: 어떤 실패에서 어떻게 동작하는지 재현 가능한 복구 기록.

### T18 — 포트폴리오와 최종 재현 절차

의존: T13~T17, F07. 수정: `README.md`, `docs/demo.md`, `docs/feature-map.md`, `.agents/skills/verify-plugpass/SKILL.md`, 본 파일.

- [ ] 문제·사용자 흐름·책임 도식·공식 데이터 출처·최신성 판단·설계 선택·한계를 실제 구현에 맞춰 정리한다.
- [ ] PR·CI·API 문서·장애 검증·성능 전후 결과를 연결하고 새 환경에서 따라 할 설정·실행·검증 명령을 확인한다.
- [ ] 최신 main에서 공용 검증과 데모 절차를 실행한다. 서비스 키 없는 재현과 실제 공급자 연동의 차이를 설명한다.
- [ ] 미완료 항목을 숨기지 않고 체크리스트를 갱신한 최종 PR을 반영한다. 공개 배포는 별도 요청이 있을 때 진행한다.
- [ ] 목록 검색→상세→대체 후보의 실제 화면·검증 기록을 포트폴리오에 연결하고 지도 후속·실공공 API 미확인·H2 한계를 구분한다.

산출: 코드만 보여 주는 대신 선택 이유·실패 조건·검증 결과를 설명할 수 있는 포트폴리오.

## 프런트 — 목록 중심 첫 버전 (계획, 미착수)

설계·공통 제약: [frontend-plan.md](docs/frontend-plan.md). 모든 프런트 파일/명령은 예정이며 지금 존재하는 것으로 취급하지 않는다. F01에서 runner·DOM 환경을 준비한 후, 제품 동작의 assertion 실패를 확인하고 구현한다. 구문·import·환경 오류를 Red로 세지 않는다. 각 작업은 대상 테스트→프런트 전체 테스트·type-check·lint·build→기존 공용 verify→필수 CI를 확인한다. 전체 verify의 로컬 실제 HTTP/E2E는 cmux 표시 규칙을 따른다.

### F01 — 검색 진입점과 프런트 검증 기반

의존: T16·T17. 생성: `frontend/index.html`, `package.json`, `package-lock.json`, `vite.config.ts`, `vitest.config.ts`, `tsconfig*.json`, `eslint.config.js`, `src/main.ts`, `src/App.vue`, `src/router/index.ts`, `src/styles/base.css`, `src/features/search/views/StationSearchView.vue`, `src/features/search/components/SearchForm.vue`. 이후 F01~F05의 축약 상대 경로는 `frontend/` 아래를 뜻한다. 수정: `.gitignore`, `.github/workflows/verify.yml`, `scripts/verify.sh`, 기술 기록·출처 목록.

계약: Vue 3·TypeScript·hash router로 `/`→`/stations` 이동, `/app/index.html`이 웹앱 진입점이며 Vite base는 `/app/`다. 초기 검색 화면은 자동 위치 권한/API 요청 없이 위치 선택·반경·커넥터·검색 버튼을 표시한다. 초기 폼 event는 F03이 연결한다.

- [ ] 공식 create-vue 구성을 확인하고 Vue/TypeScript/Vite/Pinia/Router/Axios·Vitest/VTU/jsdom·Playwright·ESLint의 실제 호환 버전과 Node를 lockfile·Node 설정에 고정한다. 기존 backend 의존성은 유지한다.
- [ ] runner·DOM 실행 준비 후 `SearchForm.spec.ts`에 ‘반경·커넥터 라벨과 검색 버튼이 보이고 위치 권한을 자동 호출하지 않는다’는 테스트를 먼저 쓴다. 최소 빈 컴포넌트를 대상으로 `npm --prefix frontend run test:unit -- src/features/search/components/SearchForm.spec.ts`를 실행해 빠진 UI assertion 실패를 확인한다.
- [ ] 필요한 초기 화면·route·기본 반경 1000m/커넥터 DC_COMBO를 구현해 대상 테스트를 통과시킨다. 알맹이 없는 scaffolding 통과를 사용자 흐름 완성으로 보고하지 않는다.
- [ ] `test:unit`(Vitest run), `type-check`(vue-tsc), `lint`, `build`, `dev` 명령과 unit JUnit 출력을 마련한다. 실제 전체 테스트·타입·lint·build를 확인하고 CI에 Node 설치와 lockfile 설치·프런트 검증을 연결한다.
- [ ] 중복 생성 예제·사용하지 않는 파일을 정리하고 재검증한다. 설치 버전·Red/Green·명령·한계를 기록한 작은 PR을 전달한다.

### F02 — URL·API 입력과 응답/오류 계약

의존: F01. 생성: `frontend/src/features/search/{types.ts,criteria.ts,api/stationApi.ts}`, `features/station-detail/{types.ts,api/stationDetailApi.ts}`, `features/recommendation/{types.ts,api/recommendationApi.ts}`, `features/charging-info/types.ts`, `shared/api/{httpClient.ts,apiError.ts}`와 같은 경로의 `.spec.ts`.

계약: `SearchCriteria={latitude,longitude,radiusMeters,connector,limit}`. `parseSearchCriteria(query: LocationQuery)`는 `empty / valid(criteria) / invalid(fields)`를 구분한다. API 함수는 `getStations(criteria, signal?)`, `getStationDetail(stationId, signal?)`, `getRecommendations(criteria, excludeStationId, signal?)`이고 Promise로 현재 DTO에 대응하는 response를 반환한다. HTTP 오류는 `validation / not-found / timeout / network / rate-limit / server / invalid-response`로 분류하고 취소는 별도 처리한다.

- [ ] `criteria.spec.ts`에 완전한 유효 query·조건 없음·부분 누락·빈 문자열·중복 값·위경도 양끝/바로 밖·NaN/Infinity·반경 100/10000과 바로 밖·limit 1/50과 바로 밖·미지원 enum·비정수 입력의 기대 결과를 각각 먼저 쓴다.
- [ ] API `.spec.ts`에 실제 `/api/v1/...` 경로·정확한 query·양수 안전 정수 상세/제외 ID·timeout 10000ms·signal 전달, 400 fields/404/429/5xx/network/timeout/취소와 잘못된 JSON/배열 누락을 검증한다. 정밀도 손실 ID는 거부하고 현재 DTO의 nullable 시각·이용 조건은 허용한다. 필수 ID/위치·반환 배열을 검증한다.
- [ ] `npm --prefix frontend run test:unit -- src/features/search/criteria.spec.ts src/shared/api/apiError.spec.ts`와 추가 API 대상 테스트에서 의도한 assertion 실패를 확인한다. 필요한 최소 선언을 갖춰 컴파일 실패와 구분한다.
- [ ] 현재 Java DTO/REST Docs 그대로 타입·query 변환·최소 응답 구조 검증·Axios 연결을 구현한다. 추천에는 좌표/dataReady가 없음을 타입에 반영한다.
- [ ] 대상·전체 검증 후 공용 충전기 타입·오류 변환의 소유권과 호출부를 리뷰하고 PR/검증 근거를 기록한다.

### F03 — 위치 선택과 주변 검색 흐름

의존: F02. 생성: `frontend/src/shared/location/useCurrentLocation.ts`, `shared/ui/RequestState.vue`, `features/search/stores/stationSearchStore.ts`, `features/search/components/StationCard.vue`, `features/charging-info/presentation.ts`, `features/charging-info/components/{StatusBadge.vue,FreshnessBadge.vue}` 및 `.spec.ts`. 수정: `SearchForm.vue`, `StationSearchView.vue`, router.

계약: `useStationSearchStore().search(criteria)`가 조회 상태/결과/요청 순번을 소유한다. `useCurrentLocation().requestLocation()`은 사용자 요청 시에만 위치를 얻고 권한 거부/실패/10초 제한을 구분한다. 확정 조건은 URL, 폼 입력은 SearchForm 소유다.

- [ ] `stationSearchStore.spec.ts`, `StationSearchView.spec.ts`, `useCurrentLocation.spec.ts`에 위치 성공·거부·timeout·늦은 위치 응답, 조건 제출·뒤로 가기 복원·직접 링크·중복 제출·API 재시도의 관찰 가능한 결과를 먼저 쓴다.
- [ ] dataReady=false/결과 없음, dataReady=false/부분 결과 있음, dataReady=true/빈 목록, 성공 목록·400 fields·network/5xx, A→B 발송/B→A 응답과 이전 실패가 새 성공을 지우지 않는 사례를 별도로 검증한다.
- [ ] 대상 명령 `npm --prefix frontend run test:unit -- src/features/search src/shared/location`에서 assertion Red를 확인한 뒤 상태 전이·위치 경계·폼 event·카드를 최소 구현한다.
- [ ] ‘검증용 예시 위치’ 37.5/127·1000m·DC_COMBO·20, 직선거리·이용 가능 보고 수·독립 상태/최신성 배지·검색 상한 표시를 연결한다. 기존 데이터가 없으면 정상 빈 결과를 보여 준다.
- [ ] 대상·전체 검증 후 요청 취소와 순번 검사의 책임·검색 조건별 결과 격리·URL 계약을 리뷰하고 실제 결과를 기록한다.

### F04 — 충전소 상세와 정보 근거 표시

의존: F03. 생성: `frontend/src/features/station-detail/stores/stationDetailStore.ts`, `views/StationDetailView.vue`, `components/ChargerList.vue` 및 `.spec.ts`. 수정: `features/charging-info/presentation.ts`·테스트, router·검색 카드 이동.

계약: `useStationDetailStore().loadStation(stationId)`가 상세 요청을 소유한다. `statusLabel(value)`, `freshnessLabel(value)`, `reasonLabel(code)`, `formatObservedTime(value)`는 한국어 표현만 맡고 서버 최신성/추천 판정을 재계산하지 않는다.

- [ ] `StationDetailView.spec.ts`에 실제 DTO 구조의 충전기 목록·404·400 ID·빈 충전기·이용 조건 누락·관측 시각 null·서로 다른 관측/수집 시각·검색 조건 보존·검색 조건 없는 직접 링크를 먼저 검증한다.
- [ ] `presentation.spec.ts`에 모든 현재 상태/최신성/사유 코드·미래의 알 수 없는 코드·잘못된 시각·UTC의 로컬 표시를 검증한다. `AVAILABLE + UNVERIFIED`가 확정 가능으로 표시되지 않는 assertion을 둔다. `stationDetailStore.spec.ts`는 빠른 ID 전환의 응답 역전을 확인한다.
- [ ] `npm --prefix frontend run test:unit -- src/features/station-detail src/features/charging-info`로 Red를 확인하고 상세·표시 규칙·조회 시점·다른 충전소 찾기를 구현한다.
- [ ] 상태 원본 문자열/비고를 HTML로 삽입하지 않고 텍스트로 표시한다. 같은 조건 새로고침 실패는 이전 정보와 실패 안내를 함께 보여 준다.
- [ ] 대상·전체 검증 후 nullable 값·공용 표시 규칙·상세/검색 이동 계약을 리뷰하고 결과를 기록한다.

### F05 — 제외 조건을 유지한 대체 후보

의존: F04. 생성: `frontend/src/features/recommendation/stores/recommendationStore.ts`, `views/AlternativeStationsView.vue`, `components/CandidateGroup.vue` 및 `.spec.ts`. 수정: 상세의 대체 후보 이동·router.

계약: `useRecommendationStore().loadAlternatives(criteria, excludeStationId)`가 후보 요청과 같은 조건의 검색 메타 요청을 조정한다. 상태는 각각 관리해 검색 메타 실패가 성공 후보를 지우지 않는다. 결과는 서버의 preferred/requiresConfirmation/excluded 순서와 그룹을 유지한다.

- [ ] `recommendationStore.spec.ts`, `AlternativeStationsView.spec.ts`에 동일 조건·정확한 제외 ID 전달·3그룹·그룹별 빈 결과·전체 빈 결과·알 수 없는 사유·직접 링크의 잘못된 제외 ID·후보 상세 이동을 먼저 쓴다.
- [ ] RECENT/UNVERIFIED/STALE의 서버 그룹을 화면이 재분류하지 않는 사례, 검색 메타만 실패해도 후보 유지, 후보 실패 재시도, 제외 ID 변경 시 응답 역전을 각각 검증한다.
- [ ] `npm --prefix frontend run test:unit -- src/features/recommendation`으로 assertion Red를 확인하고 최소 카드·사유 설명·제외 그룹 접어 보기·독립 재시도를 구현한다.
- [ ] 추천 카드에 없는 좌표/충전기 수/전체 건수를 만들어 표시하지 않는다. 클릭 시 실제 상세 조회로 연결한다.
- [ ] 대상·전체 검증 후 검색·상세·대체 후보를 통과하는 URL 조건·원본 ID 계약과 책임을 리뷰하고 결과를 기록한다.

### F06 — 실제 백엔드 연결과 화면에 보이는 E2E

의존: F05. 생성: `src/test/java/e2e/ChargingDemoApplication.java`, `DemoProvider.java`, `ChargingDemoApplicationTests.java`, `src/test/resources/publicdata/browser-demo.xml`, `frontend/playwright.config.ts`, `frontend/e2e/{charging-journey.spec.ts,ui-failures.spec.ts}`, `scripts/frontend-e2e.sh`, `docs/frontend-verification.md`. 수정: build.gradle의 test-classpath 실행 경로·frontend scripts·공용 JAR 검사·필수 Java suite 목록.

계약: 운영 Controller/Service/Repository와 H2를 사용하고 fixture HTTP 공급자·시간만 제어한다. test-classpath 데모 앱이 실제 수집을 호출한다. UI 실패 테스트와 실제 저장소 연결 E2E는 서로 다른 spec/보고서로 구분한다. 서버 시작/종료 도우미는 자신의 PID·포트·준비 상태만 관리한다.

- [ ] 데모 서버 테스트를 먼저 작성해 합성 2곳 수집·검색의 dataReady/ID·상세의 UNVERIFIED·첫 ID 제외 후 다른 후보만 반환을 실제 HTTP로 assertion한다. 테스트 시작 준비를 갖춘 뒤 의도한 Red를 확인하고 데모 외부 경계를 구현한다.
- [ ] `charging-journey.spec.ts`를 먼저 작성해 ‘검증용 예시 위치’→검색→첫 상세→대체 후보→다른 상세의 텍스트·ID·제외 query·시각 누락 안내를 assertion한다. UI 실패 spec에는 권한 거부·empty·400/404/429/5xx·timeout·잘못된 응답·응답 역전을 별도로 둔다.
- [ ] AGENTS.md의 현재 cmux 식별·보조 pane 규칙을 적용하고 저장소·흐름을 먼저 설명한다. 실패/통과 러너와 서버 로그를 해당 pane에서 실행한다. `npm --prefix frontend run test:e2e -- --headed` 또는 UI 러너를 사용하며 종료 코드와 실제 assertion을 확인한다.
- [ ] 같은 workspace의 cmux browser에서도 실제 앱에 입력·클릭·이동해 연결 흐름을 보인다. mock UI spec과 실제 DB/HTTP 흐름의 범위 차이를 기록한다. 서버는 cmux 확인 동안 별도 serve 모드로 유지할 수 있게 한다.
- [ ] 도우미의 초기화/실행 실패가 0 종료로 숨겨지지 않고 운영 JAR에는 `e2e/` 클래스가 없음을 확인한다. 직접 시작한 서버만 종료하고 pane은 남긴다. 실제 cmux 불가 시 원인·대체 범위·미확인을 기록한다.
- [ ] 대상·프런트 전체·기존 공용 검증을 확인하고 JUnit/HTML/trace·서버 로그·source/버전과 Red/Green 근거를 연결한 PR을 전달한다.

### F07 — 웹앱 패키징·모바일·접근성·책임 경계·필수 CI

의존: F06. 수정: `build.gradle`, `frontend/src/styles/base.css`, 각 View/Component·관련 테스트, `frontend/eslint.config.js`, `scripts/verify.sh`, `scripts/verify-runtime.py`·검사기 회귀 테스트, `.github/workflows/verify.yml`, `docs/feature-map.md`, `.agents/skills/verify-plugpass/SKILL.md`, `docs/frontend-verification.md`.

계약: 필수 `PlugPass verify`가 backend와 frontend의 실제 테스트·타입·lint·build·전체 연결 E2E를 확인한다. existing verifier를 확장하며 별도 중복 CI/coverage 목표를 만들지 않는다. 로컬 E2E는 화면 표시, CI는 headless다.

- [ ] 모바일 375×812·데스크톱 1440×900, 키보드만 검색/상세/후보 이동, label·오류 focus·로딩 알림·색 외 상태 텍스트의 관찰 가능한 회귀 사례를 먼저 작성·실행한다. 필요한 UI만 수정해 대상·전체 검증한다.
- [ ] Vue→Axios 직접 import, API→Store/View 역의존을 ESLint에서 거부하도록 허용/금지 예제를 먼저 검증한다. 실제 소스에 임시 위반을 넣어 공용 검사 실패를 확인하고 복원 후 성공을 확인한다.
- [ ] 기존 XML 검사기에 프런트 JUnit의 필수 suite 누락·0개·skip·failure·error 거부를 확장한다. 회귀 테스트를 먼저 실패시키고 최소 구현한다. 고정 총건수·의미 없는 coverage 목표는 도입하지 않는다.
- [ ] 실제 JAR의 `/app/index.html`·참조 JS/CSS·직접 링크 새로고침·같은 origin API·기존 `/docs/index.html`을 먼저 검증해 누락 assertion의 Red를 확인한다. Gradle 프런트 build→bootJar의 `static/app/` 포함을 구현하고 실제 HTTP로 재검증한다. 개발 서버 E2E와 JAR 파일 제공 검증의 범위를 구분한다.
- [ ] 기존 `PlugPass verify` 안에서 lockfile 설치→프런트 unit/type-check/lint/build→UI 실패 spec·실제 백엔드 E2E를 실행한다. 원본 보고서를 보존하며 job timeout은 측정 후 필요할 때만 조정한다. `continue-on-error`·전체 skip 통과를 사용하지 않는다.
- [ ] 현재 소스에서 로컬 공용 검증·원격 필수 CI·화면 E2E 범위를 확인하고 기능 지도/검증 스킬의 예정 경로를 실제 관측 결과로 바꾼다. 구현하지 않은 지도·실공공 API 인증·운영 배포는 완료 표시하지 않는다.
- [ ] 이름→실제 수행→배치·상태 소유권·호출부/JSON 계약을 리뷰하고 기록한다. T18로 최종 포트폴리오 작업을 넘긴다.

### F08 — 지도와 목록 연결 (후속, 첫 버전에서 제외)

의존: F07·별도 지도 설계. [후속 지도 범위](docs/frontend-plan.md)의 SDK 조건·마커/카드 동기화·SDK 실패 시 목록 유지·추천 좌표 한계를 따른다.

- [ ] 지도 SDK·공식 사용 조건·허용 도메인/키·출처 표시·지원 브라우저를 확인한다. 주소 검색/경로 안내·추천 지도까지 필요한지 범위를 따로 결정한다.
- [ ] 확정한 범위의 마커 선택·목록 선택·조건 변경·SDK 실패를 TDD로 검증하고 현재 cmux에서 실제 흐름을 확인한다.

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
