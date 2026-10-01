# PlugPass 작업 체크리스트

**목표·설계:** [plan.md](plan.md). 업무 기능은 아직 미구현이며 아래 T01부터 진행한다.

**실행 방식:** 작업 하나씩 순서대로 진행한다. 구현 시 `superpowers:executing-plans`를 사용한다. 이 파일을 만들었다는 이유로 이후 작업 전체를 자동 실행하지 않는다.

**공통 제약:** Java 25·Spring Boot 4.1.1·JPA/H2·Spring REST Docs, 단일 인스턴스·공급자 1곳·검증 지역 1곳. 공식 계약을 먼저 확인한다. 현재 H2는 메모리 모드다. 작은 PR·필수 CI·자동 머지와 cmux 검증 정책을 유지한다.

## 체크 방법

- `[x]`는 구현·실행 검증·main 반영 근거가 있는 완료 항목이다. 계획·fixture만으로 실연동을 완료 처리하지 않는다.
- T번호 하나가 기본 PR 단위다. 의존 작업이 main에 들어온 후 시작한다. 완료 시 해당 T항목에 PR·검증 결과 링크를 남긴다.
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

의존: 없음. 파일: `docs/data-contract.md`, `src/test/resources/publicdata/normal.json`, `empty.json`, `unknown-status.json`(실제 포맷이 XML이면 확장자도 변경).

- [ ] 공식 API를 하나 선정하고 공식 URL·신청/인증·페이지 규칙·호출 한도·갱신 특성·이용 조건·확인일을 기록한다.
- [ ] 검증 지역 하나와 충전소/충전기 식별자, 좌표, 커넥터, 이용 제한, 상태 코드, 각 시각 필드의 의미·시간대를 실제 응답으로 확인한다.
- [ ] 개인정보·서비스 키가 없는 fixture를 만들고 실제 연동 확인 여부와 미제공 필드를 구분한다. 최신성 `maxAge`와 요청/재시도 예산의 근거를 기록한다.
- [ ] 실응답과 가이드를 대조하고 fixture 구문·필수 식별자·비밀 정보 미포함을 검사한다. 계약 PR과 확인 결과를 연결한 뒤 완료한다.

산출: 이후 작업에서 사용할 외부 계약과 재현 입력. 접근 불가이면 원인과 진행 가능한 범위를 남기고 실연동 확인 체크는 열어 둔다.

### T02 — 충전소·충전기 식별과 위치 모델

의존: T01. 파일: `station/Station.java`, `Charger.java`, `ChargerId.java`, `GeoPoint.java`(main 패키지 아래), `station/StationModelTests.java`(test 패키지 아래).

- [ ] 다른 공급자·충전소의 같은 충전기 번호가 충돌하지 않는 테스트, 빈 식별자·범위 밖 좌표를 거부하는 테스트를 먼저 작성한다.
- [ ] `ChargerId(provider, stationId, chargerId)`와 `GeoPoint(latitude, longitude)`를 정의하고 Station/Charger가 각자의 데이터 유효성을 지키게 한다.
- [ ] `./gradlew test --tests '*StationModelTests'`에서 실패 확인 후 구현·통과시키고 PR을 반영한다.

산출: 수집·저장의 공통 식별자와 유효한 위치 모델. 외부 API DTO를 업무 객체로 직접 사용하지 않는다.

### T03 — 공급자 상태 코드 정규화

의존: T01, T02. 파일: `station/ChargerStatus.java`, `ingestion/ProviderStatusMapper.java`, `ingestion/ProviderStatusMapperTests.java`.

- [ ] 정상 코드·알 수 없는 코드·null을 입력하는 테스트를 작성한다. 미지원 값은 AVAILABLE이 아니라 UNKNOWN이어야 한다.
- [ ] `ProviderStatusMapper.map(String rawCode): ChargerStatus`와 AVAILABLE/OCCUPIED/UNAVAILABLE/UNKNOWN을 구현하고 원본 코드를 보존한다.
- [ ] `./gradlew test --tests '*ProviderStatusMapperTests'`의 실패·통과를 확인하고 계약 표와 함께 PR을 반영한다.

산출: T01 코드표에 근거한 상태 변환. 운영 시간·접근 제한은 상태 코드 하나로 추정하지 않는다.

### T04 — 중복 없는 업무 데이터 저장

의존: T02, T03. 파일: `station/StationRepository.java`, `station/ChargerRepository.java`, `ingestion/StationUpsertService.java`, `ingestion/StationSnapshot.java`, `station/StationPersistenceTests.java`, `ingestion/StationUpsertTests.java`. 수정: Station/Charger JPA 매핑.

- [ ] Repository 매핑·쿼리는 기본 save 후 조회로 검증한다. 매핑 복원이 목적일 때만 이유를 명시해 flush/clear하며 참조 차이 자체를 assertion하지 않는다.
- [ ] 반복 저장·식별자 충돌·역순 응답은 실제 StationUpsertService 트랜잭션 commit 후 별도 조회로 검증한다. 테스트 Transactional 없이 AfterEach에서 생성 데이터만 FK 역순으로 정리한다.
- [ ] 유일 제약과 `StationUpsertService.upsert(StationSnapshot snapshot)`을 구현한다. `StationSnapshot`은 T01 필드의 정규화 입력이며 시각의 의미를 보존한다.
- [ ] 신뢰할 수 있는 공급자 순서 시각이 있으면 이전 관측의 덮어쓰기를 거부한다. 없으면 순서를 보장할 수 없음을 기록하고 T06의 직렬 수집으로 제한한다.
- [ ] `./gradlew test --tests '*StationPersistenceTests' --tests '*StationUpsertTests'`와 전체 테스트·공용 검증을 통과하고 PR을 반영한다.

산출: 업무용 JPA 모델과 갱신 경계. 기존 PersistenceProbe 테스트를 업무 구현 완료의 근거로 대신하지 않는다.

### T05 — 외부 API 클라이언트

의존: T01, T03, T04의 StationSnapshot 계약. 파일: `ingestion/PublicDataClient.java`, `ingestion/StationPage.java`, `ingestion/PublicDataClientTests.java`, 외부 페이지 DTO. 수정: `application.properties`의 외부 설정 참조.

- [ ] fixture HTTP 서버로 정상·빈 페이지·페이지 마지막·잘못된 본문·인증 오류를 테스트한다. 서비스 키가 로그에 나오지 않는지도 확인한다.
- [ ] `PublicDataClient.fetchPage(int page): StationPage`를 구현한다. 페이지 번호 기준과 종료 신호는 T01 계약을 따르고 결과는 StationSnapshot 목록으로 변환한다.
- [ ] 연결·응답 timeout과 인증 값의 외부 주입을 적용한다. 공개 수집 실행 API는 추가하지 않는다.
- [ ] `./gradlew test --tests '*PublicDataClientTests'`를 통과하고 정상·오류 계약을 문서화한 PR을 반영한다.

산출: 외부 I/O 경계. 테스트 서버 통과와 실제 공공 API 인증 성공을 구분한다.

## 2주차 — 수집부터 사용자 조회까지

### T06 — 한 번의 수집 실행과 부분 실패 기록

의존: T04, T05. 파일: `ingestion/StationSyncService.java`, `SyncResult.java`, `SyncRun.java`, `SyncRunRepository.java`, `ingestion/StationSyncTests.java`.

- [ ] 여러 페이지 수집, 같은 응답 재실행, 중간 페이지 실패, 잘못된 레코드가 섞인 경우를 테스트한다.
- [ ] `StationSyncService.synchronize(): SyncResult`로 페이지를 순차 처리하고 페이지별 짧은 저장 트랜잭션을 사용한다. 네트워크 대기 중 DB 트랜잭션을 열어 두지 않는다.
- [ ] 실행 ID·시작/종료·처리/실패 건수·결과를 저장한다. 전체 성공만 lastSuccessfulRunAt을 바꾸고, 부분 실패에서 기존 행을 삭제하지 않는다.
- [ ] `./gradlew test --tests '*StationSyncTests'`와 공용 검증으로 저장 결과를 확인하고 PR을 반영한다.

산출: 반복 호출해도 중복되지 않는 수집 진입점과 성공/부분 실패/실패를 구분한 SyncResult.

### T07 — 관측 시각에 근거한 최신성 판정

의존: T01, T03, T06. 파일: `freshness/Freshness.java`, `FreshnessPolicy.java`, `freshness/FreshnessPolicyTests.java`.

- [ ] 고정 Clock으로 경과 시간 maxAge 미만·같음·초과, 미래/누락 시각, 재수집한 오래된 응답을 테스트한다.
- [ ] `FreshnessPolicy.evaluate(Instant sourceObservedAt, Instant now): Freshness`로 RECENT/STALE/UNVERIFIED를 반환한다. maxAge와 같은 경계는 RECENT다.
- [ ] collectedAt이나 상태 변경 시각을 관측 시각 대신 사용하는 경로를 막는다. 신뢰 가능한 관측 시각이 없는 공급자는 UNVERIFIED와 이유를 반환한다.
- [ ] `./gradlew test --tests '*FreshnessPolicyTests'`를 통과하고 판정 기준을 설명하는 PR을 반영한다.

산출: 외부 I/O 없는 최신성 정책. null 관측 시각은 UNVERIFIED이며 미래 시각도 RECENT가 아니다.

### T08 — 주변 충전소 조회 API

의존: T04, T07. 파일: `search/StationQueryService.java`, `StationController.java`, `StationSearchQuery.java`, `search/StationSearchTests.java`. 수정: REST Docs 문서.

- [ ] 위치·반경 경계 포함, 호환 커넥터, 거리 동률, 빈 결과, 잘못된 좌표/반경/limit의 HTTP 테스트를 작성한다.
- [ ] `GET /api/v1/stations`와 `StationQueryService.search(StationSearchQuery query)`를 구현한다. plan.md의 입력 범위·기본 limit·거리/ID 순서를 적용한다.
- [ ] 개별 충전기의 상태·최신성을 조합한 요약을 반환한다. 정상 초기 수집 전에는 빈 목록과 데이터 준비 상태를 구분해 제공한다.
- [ ] `./gradlew test --tests '*StationSearchTests'`와 REST Docs·공용 검증을 통과하고 PR을 반영한다.

산출: 외부 API를 호출하지 않는 저장 데이터 기반 검색. 지도 프런트엔드는 필요하지 않다.

### T09 — 충전소 상세와 판단 근거

의존: T08. 파일: `search/StationDetailTests.java`, 상세 응답 타입. 수정: StationController/StationQueryService와 REST Docs 문서.

- [ ] 존재하는 충전소·없는 ID·여러 상태의 충전기·누락된 운영 정보에 대한 테스트를 작성한다.
- [ ] `GET /api/v1/stations/{stationId}`에 충전기 상태·원본 코드·관측/수집 시각·최신성·이용 제한·미확인 이유를 제공한다. 없는 ID는 404다.
- [ ] `./gradlew test --tests '*StationDetailTests'`와 API 문서·공용 검증을 통과하고 PR을 반영한다.

산출: “왜 이 충전소를 믿거나 다시 확인해야 하는가”를 보여 주는 조회 응답.

## 3주차 — 대체 후보와 안정적인 반복 수집

### T10 — 근거가 있는 대체 후보

의존: T08, T09. 파일: `recommendation/CandidatePolicy.java`, `StationCandidate.java`, `CandidateGroups.java`, `RecommendationController.java`, `recommendation/RecommendationTests.java`. 수정: REST Docs 문서.

- [ ] 호환되지 않는 커넥터, 명시적 이용 제한, stale/unknown, 제외한 충전소, 후보 0개, 거리 동률을 테스트한다.
- [ ] `CandidatePolicy.rank(List<StationCandidate> candidates): CandidateGroups`와 `GET /api/v1/recommendations`를 구현한다. StationSearchQuery로 검색한 뒤 제외 ID를 빼고, 추천 판단에 필요한 값만 StationCandidate로 전달한다.
- [ ] RECENT·AVAILABLE 후보와 UNVERIFIED인 확인 필요 후보를 분리하고 reasonCodes를 제공한다. 미확인 운영 조건은 경고로 표시하며 STALE을 확정 이용 가능으로 올리지 않는다.
- [ ] `./gradlew test --tests '*RecommendationTests'`와 REST Docs·공용 검증을 통과하고 PR을 반영한다.

산출: plan.md의 결정 가능한 규칙에 따른 후보. 예측 정확도나 충전 성공률을 임의로 표시하지 않는다.

### T11 — 중복 실행을 막는 수집 스케줄

의존: T06. 파일: `ingestion/SyncScheduler.java`, `ingestion/SyncSchedulerTests.java`. 수정: 수집 활성화·간격 설정.

- [ ] 아직 실행 중인 수집을 다시 호출할 때 중복되지 않고, 예외 종료 후 다음 실행이 가능한지 테스트한다.
- [ ] 단일 인스턴스용 실행 소유권을 두고 고정 지연 방식으로 synchronize를 호출한다. 수집은 기본 비활성화하며 명시적 설정과 인증이 있을 때 켠다.
- [ ] T01의 호출 한도와 한 회차의 페이지 수로 간격을 정한다. `./gradlew test --tests '*SyncSchedulerTests'`에서 실제 장시간 sleep 대신 제어 가능한 실행기를 사용하고 PR을 반영한다.

산출: 한 프로세스에서 중복되지 않는 반복 수집. 분산 락·다중 인스턴스 보장은 하지 않는다.

### T12 — 외부 장애의 시간·재시도 제한과 복구

의존: T05, T06, T11. 파일: `ingestion/IngestionRecoveryTests.java`. 수정: PublicDataClient/StationSyncService의 실패 분류와 실행 예산.

- [ ] timeout, 429, 5xx, 401/403, 깨진 본문, 페이지 무진행을 주입한다. 종료까지 요청 횟수·총 소요 예산에 상한이 있는지 확인한다.
- [ ] 일시 오류만 제한 재시도하고 인증·계약 오류는 같은 요청을 반복하지 않는다. Retry-After는 남은 실행 예산 안에서 적용한다.
- [ ] 장애 중 조회가 기존 데이터를 반환하되 최신성은 시간에 따라 낮아지는지, 공급자 복구 후 다음 수집이 갱신하는지 테스트한다.
- [ ] `./gradlew test --tests '*IngestionRecoveryTests'`와 공용 검증을 통과하고 실패 분류·복구 설명을 포함한 PR을 반영한다.

산출: 무한 재시도 없이 끝나고 다음 실행에서 회복하는 수집. 프로세스 재시작 뒤 H2 데이터 복구와는 구분한다.

### T13 — 수집 상태와 데이터 품질 관측

의존: T06, T11, T12. 파일: `ingestion/IngestionMetrics.java`, `ingestion/IngestionMetricsTests.java`, `docs/operations.md`.

- [ ] 성공/부분 실패/실패, 재시도, 마지막 성공 이후 경과, 조회 데이터의 최신성 분포를 구분하는 검증을 작성한다.
- [ ] 실행 ID·처리 건수·실패 원인을 로그와 지표로 연결한다. 비밀 키·응답 원문 전체를 로그에 남기지 않고 충전소 ID를 지표 태그로 사용하지 않는다.
- [ ] `./gradlew test --tests '*IngestionMetricsTests'`를 통과하고 health와 데이터 최신성의 차이·운영 확인 명령을 문서화한 PR을 반영한다.

산출: 서버 정상과 데이터 수집 정상의 차이를 관찰하는 방법. 외부 모니터링 서비스 전송은 이 작업의 필수 범위가 아니다.

## 4주차 — 연결된 흐름과 성능 기준선

### T14 — 수집부터 대체 후보까지 실제 HTTP 검증

의존: T07~T13. 파일: `src/test/java/com/plugpass/ChargingJourneyTests.java`, `docs/demo.md`. 수정: `scripts/verify-runtime.py`, 기능 지도·검증 스킬.

- [ ] 현재 cmux workspace·surface와 전용 pane을 확인해 실행 명령·로그·실제 요청을 보이게 준비한다. 연결 불가 시 원인과 대체 범위를 기록한다.
- [ ] 제어 가능한 공급자 응답을 수집한 후 실제 HTTP로 주변 조회 → 상세 → 첫 충전소 제외 → 대체 후보 조회를 수행하고 값까지 assertion한다.
- [ ] 같은 데이터 재수집, 시간 경과 후 stale, 외부 장애, 복구 후 갱신, 후보 없음도 확인한다. 실제 공공 API 표본 검증은 별도 증거로 남긴다.
- [ ] `./gradlew test --tests '*ChargingJourneyTests'`와 업데이트된 공용 검증을 통과하고 결과·한계를 포함한 PR을 반영한다.

산출: 직접 재현할 수 있는 핵심 흐름. fixture 공급자와 실제 공급자 검증을 서로 대신하지 않는다.

### T15 — 조회 성능 기준선 측정

의존: T08, T10, T14. 파일: `scripts/performance/`의 선택한 부하 도구 실행 파일, `docs/performance.md`.

- [ ] T14의 응답 정확성 assertion을 유지하면서 생성 데이터·시드·실행 환경·조회 분포를 고정하고 부하 도구 하나를 선택한다.
- [ ] plan.md의 10,000개 충전소·50,000개 충전기·동시 사용자 20명·30초 준비·3분 측정 조건을 실행 가능한 명령으로 만든다.
- [ ] p50/p95·예상하지 않은 오류율·조회별 SQL 수·병목과 목표(p95 300ms, 오류율 1% 미만) 달성 여부를 기록한다. 생성 데이터 결과임을 표시한다.
- [ ] 명령·원본 결과 위치·대상 SHA를 포함한 측정 PR을 반영한다. 측정 전후 데이터 정확성도 확인한다.

산출: 개선 여부를 판단할 재현 가능한 기준선. 숫자를 측정 없이 완료로 채우지 않는다.

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

의존: T13~T17. 수정: `README.md`, `docs/demo.md`, `docs/feature-map.md`, `.agents/skills/verify-plugpass/SKILL.md`, 본 파일.

- [ ] 문제·사용자 흐름·책임 도식·공식 데이터 출처·최신성 판단·설계 선택·한계를 실제 구현에 맞춰 정리한다.
- [ ] PR·CI·API 문서·장애 검증·성능 전후 결과를 연결하고 새 환경에서 따라 할 설정·실행·검증 명령을 확인한다.
- [ ] 최신 main에서 공용 검증과 데모 절차를 실행한다. 서비스 키 없는 재현과 실제 공급자 연동의 차이를 설명한다.
- [ ] 미완료 항목을 숨기지 않고 체크리스트를 갱신한 최종 PR을 반영한다. 공개 배포는 별도 요청이 있을 때 진행한다.

산출: 코드만 보여 주는 대신 선택 이유·실패 조건·검증 결과를 설명할 수 있는 포트폴리오.

## 작업마다 공통으로 확인할 완료 조건

- [ ] 이번 PR이 해결하는 동작 하나와 의존 작업을 설명했다.
- [ ] 구현 변경은 의도한 assertion 실패로 Red를 확인했고 Green·Refactor 후 대상·전체 테스트를 실행했다.
- [ ] 성공·예외·실패·엣지 케이스와 이름·책임·공개 계약 검토 결과를 기록했다.
- [ ] 필요한 REST Docs·기능 지도·검증 절차를 함께 갱신했다.
- [ ] 로컬 E2E의 cmux 표시 여부와 실제 검증 범위를 기록했다.
- [ ] 현재 PR head의 필수 CI를 확인하고 자동 머지 완료를 확인했다.
- [ ] 관련 T항목에 PR·검증 근거를 연결했다.

이 마지막 목록은 매 작업에 복사해서 사용하는 템플릿이며 전체 프로젝트의 진행률 집계에는 포함하지 않는다.
