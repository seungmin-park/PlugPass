# 기능과 검증 범위

## T28 모바일 지도 앱 길찾기

`상세 API → StationDetailView → StationDirections → navigationLinks → OS/외부 앱`.
상세 Store가 현재 충전소와 조회 수명을 소유한다. T40부터 상세 화면의 길찾기 버튼이 앱 선택 영역을 열며, 선택한 지도 앱은 다음 길찾기의 직접 링크가 된다. 링크 모듈은 유한 좌표·이름 검증, 인코딩, OS별 scheme/intent·설치 URL·웹 대안을 만든다. 검색 좌표를 출발지로 보내지 않는다. 이전 상세의 링크는 다음 요청 시작 시 제거된다.

## T40 검색 결과에서 바로 길찾기

```text
카드 길찾기 ─┬─ 첫 선택 → 하단 패널 → 지도 앱 선택 → 선호 저장
             └─ 다음 선택 → 저장한 앱으로 직접 연결
지도 마커 → 같은 충전소를 선택 → 하단 패널 → 길찾기·이용 조건
```

검색 View는 카드·마커·패널에 공통 충전소 ID 하나를 전달한다. `StationActionSheet`는 사용 조건 요약과 닫기/초점을, `StationDirections`는 선택 UI를, `navigationLinks`는 OS별 목적지 URL을, 선호 Store는 현재 선택과 저장소 복구를 맡는다. 저장소가 막혀도 현재 페이지에서 선택한 앱으로 연결한다. 좌표가 유효하지 않으면 링크를 만들지 않는다. 외부 앱이 실제 설치됐는지는 브라우저가 판정할 수 없으므로 설치·웹 대안을 제공한다. [Red·Green·전체 검증](t40-verification.md).

## T41 이용 조건과 시각의 의미

```text
공급자 원본·서버 응답 → 표시 정책 → 충전기 요약: 상태·운영 시간·출입 조건·현장 관측
                                  └→ 펼치는 출처: 원본 코드·서버 수집·상태 변경 시각
```

`presentation`은 공급자 이용 제한 문구와 관측 시각의 누락·파손을 사용자 안내로 바꾼다. 상세의 `ChargerList`와 지도 선택 패널은 같은 문구를 사용한다. 서버의 최신성·추천 판정은 UI에서 다시 계산하지 않는다. `sourceStatusChangedAtRaw`는 상태 변경 시각이며 시간대와 현장 관측 의미를 추정하지 않는다. 충전소 식별자와 조회 시각은 별도 출처 영역에서 확인할 수 있다. [실행 근거](t41-verification.md).

## T42a 여러 연결 규격의 검색·추천

```text
connector 한 값 / connectors 여러 값 → 요청 검증 → ConnectorSelection
                                                   ├─ 공급자 코드 합집합 → 충전기 조회·충전소 정렬
                                                   └─ 같은 선택 → 추천의 호환 판단
```

두 입력을 함께 보내면 400이다. 합집합 SQL 조회에서 조합형 충전기 한 대는 한 번만 반환하고, 호환 필터와 정렬 뒤 limit을 적용한다. 기존 단일 규격 URL과 응답 구조는 유지한다. 검색/지도 영역/추천 세 요청이 같은 입력 규칙을 사용한다. 서버는 차량과 어댑터 보유 여부를 추정하지 않는다. [실행 근거](t42a-verification.md).

## T42b 차량 정보와 이번에 사용할 어댑터

```text
제조사 근거가 있는 차량 / 목록에 없는 차량 → SearchForm
        ├─ 차량 카탈로그 → VehicleCompatibility → 직접 규격
        ├─ 이번에 사용할 어댑터 + 차량 지원 확인 → 어댑터 규격
        └─ 직접 확인한 충전구 → 기존 단일 규격
선택 조건 → URL·SearchCriteria → 검색/지도 영역/추천 API → 결과·상세·후보 복귀
```

`vehicleCatalog`는 확인한 국내 차량 자료와 출처·확인일을, `vehicleCompatibility`는 직접/어댑터/확인 필요 판단을 소유한다. 화면은 어댑터 보유를 추정하지 않고 이번 사용과 차량 지원 확인을 받아야 검색에 포함한다. 현재 자동 확정 차량은 국내 2020 코나 일렉트릭의 DC 콤보뿐이다. Tesla 국내 차량은 연식·개별 차량 충전 사양을 자동 확정하지 않으며 사용자가 지원을 확인한 공식 어댑터 규격만 추가한다. 목록에 없는 차량은 설명서를 확인한 뒤 직접 규격을 선택한다. `vehicleProfile`은 저장 실패·오래된 ID를 안전하게 처리한다. 검색 URL은 단일 `connector`와 복수 `connectors`를 모두 복원하고, 결과에는 직접/선택 어댑터 조건을 설명한다. [실행 근거](t42b-verification.md).

`StationNavigation.spec.ts`12건과 `navigationLinks.spec.ts`19건: Android/iOS/iPadOS/desktop, 목적지·특수문자·좌표 경계·로딩/실패·교체. `navigation.spec.ts`3건: 실제 수집/DB 상세→선택→앱 전달 URL→웹 새 탭→검색 복귀·다른 목적지와 키보드 조작. 개발 E2E와 재시작된 MySQL을 사용하는 운영 JAR에서 같은 spec을 실행한다. 앱 실행 경계만 가로채므로 실제 휴대폰 앱 실행은 미확인이다. [검증·실패·한계](t28-verification.md).

## T29 처리 중 시각적 표시

`기존 API Store/위치 composable/지도 상태 → LoadingIndicator(message) → 회전 원 + 작업 문구`.
검색·상세·후보·수집 상태·위치·지도 대기 중 공용 표현을 사용한다. 성공/오류/취소 후 제거·재조회 때 다시 표시하고 감소 설정은 회전만 멈춘다. 상태/취소/timeout 소유권은 기존 객체에 유지한다. `RequestState.spec.ts`와 `LoadingFlows.spec.ts`, 지도 테스트가 수명 계약을, `loading.spec.ts`가 실제 CSS 회전·감소 모드·모바일·503 복구를 검증한다. 기존 timeout E2E도 spinner 제거를 확인한다. [Red·Green·실행 범위](t28-verification.md#t29-회전-로딩-표시).

## 운영 DB 전환 (2026-10-12, T26/T27)

사용자 확정 DB는 MySQL8.4 LTS다. [계획](mysql-deployment-plan.md)·[설정](operations.md#mysql-배포-프로필)을 따른다.

| 경로 | 책임·기대 결과 | 실행 가능한 검증 |
| --- | --- | --- |
| deployment 기동→스키마 준비 | Flyway V1 생성/이력, Hibernate validate, DB 주소·암호 외부 주입 | `python3 scripts/verify-mysql.py --tests-only`; 실제 MySQL·업무 엔티티/Repository |
| 수집→저장·재수집 | 기존 StationUpsertService transaction과 도메인 identity 유지 | MySqlDeploymentTests: unicode/UTC/대소문자/공백/선행0·중복 갱신·child 실패 rollback·unique/FK·schema drift 거부 |
| 앱 종료·DB 재시작→배포 JAR 조회 | station/charger/sync_run 보존, 재수집 없이 dataReady와 마지막 완료 시각 유지 | `python3 scripts/verify-mysql.py`; `build/verification/mysql/{before,after,result}.json`·서버 로그 |
| 재시작 JAR 웹앱→검색·상세·대체 후보 | 같은 origin API, 첫 ID 제외, UNVERIFIED 유지 | 기존 charging-journey Playwright를 MySQL JAR에 실행; `frontend/test-results/mysql.json`; cmux 직접 조작은 작업 기록 |

기본 프로필과 기존 회귀는 H2를 유지한다. MySQL 검증은 공용 verify·필수 CI에 연결하며 Docker 미사용·0건·실패·skip을 성공으로 취급하지 않는다. 실제 운영 인프라 생성/데이터 이관/백업 복원/전체 실공공 수집·MySQL 부하 성능은 이 로컬 저장 계약과 구분한다.

## T01 인증된 공급자 계약 (2026-10-06)

인천 실공공 API의 첫/마지막/빈 페이지를 실제 `DefaultPublicDataClient`로 확인했다. XML parser는 보고 건수·목록·요청 범위를 검증하고, `StationPage`는 요청 용량을 보존해 `hasNext`와 `SyncProgress`의 일관성 검사를 유지한다. 마지막6건·빈0건을 요청 용량10으로 계산하는 회귀, 잘못된 보고값·전체 범위 초과·조기 빈 페이지를 포함해 `PublicDataClientTests` 현재74건이다. [계약](data-contract.md)·[실표본과 TDD/실행 근거](t01-live-verification.md)를 따른다. 인증키가 없는 fixture 기반 회귀와 실제 공급자 호출을 구분하며 전체 실데이터 수집·시간대·현장 좌표 정확성은 보장하지 않는다.

## 프런트 경로 (2026-10-06, T18~T25 웹앱·지도·실제 연결·JAR·품질 검사)

사용자는 목록 중심 반응형 웹앱으로 검색·상세·대체 후보를 첫 버전으로 선택했다. [프런트 계획](frontend-plan.md)·[작업 목록](../tasks.md)의 T18~T25을 따른다. 위치→검색→상세→후보→다른 상세는 같은 cmux 브라우저에서 실제 API/DB 연결을 확인했다. Playwright 실제 연결·실패 화면 10건과 진단 거부는 T23에서 확인했다. T24에서 JAR웹앱과 공용verify/CI 브라우저를 연결했다. T25는 검색 결과 지도·목록 선택을 추가하며 실제 실행과 전달 상태는 [T25 기록](t25-verification.md)을 따른다.

| 사용자 경로 | 상태 소유자·경계 | 기대 결과·검증 범위 |
| --- | --- | --- |
| 검색 결과→지도 보기→마커/카드 선택 | 검색 View의 선택 ID, StationMap의 수명/실패, 카카오 지도 adapter의 DOM/이벤트 | 선택·조건 초기화, SDK 다운로드/배경 준비 실패, 키보드·375/1440px를 개발·패키지 Chromium에서 검증한다. 운영 카카오 SDK·지도 이미지도 확인했다. 과거 지도 검증은 [T25 실행](t25-verification.md), 현재 제거 범위는 [T44c](t44c-remove-leaflet.md). |
| 타입·템플릿·실행 경고 검사 | tsconfig·package 명령·단위 setup·E2E fixture·공용 verify | 임시 타입/lint/Vue 경고/예외 거부·복원·테스트별 격리; [T18/T23/T24 계획](frontend-plan.md#101-엄격한-정적-검사와-vue-경고), 개발 모드와 JAR 검증 구분 |
| 위치·반경·커넥터 선택→검색 | URL 확정 조건, SearchForm 입력, stationSearchStore 비동기 상태, stationApi HTTP | 위치14·Store7·View14건, 준비 전/부분/빈 결과·권한 거부·400·응답 역전. [T20 실제 실행](t20-verification.md); cmux 실제 검색/뒤로 가기/직접 링크 |
| 검색→상세·직접 링크 | route ID, stationDetailStore, 공용 charging-info 표시 | Store6·View10·충전기3건; null·404·unknown·텍스트 비고·응답 역전. [T21 실제 실행](t21-verification.md) |
| 상세→현재 충전소 제외 추천→다른 상세 | URL 조건/제외 ID, recommendationStore, 서버 후보 그룹 | Store6·View10·그룹2건, 독립재시도·성공/실패 응답역전. [T22 실제 실행](t22-verification.md) |
| fixture 수집→실제 API→브라우저 클릭 | 테스트 외부 공급자/시간, 실제 Service/Repository/H2 | Playwright 실제 연결 1·UI 실패9건, 개발모드 진단3종 거부·복원; [T23 실행](t23-verification.md) |
| 모바일·키보드·필수 CI | 표현 컴포넌트, ESLint, 기존 공용 verify | 화면7건·개발E2E17건·JAR2건, 0개/skip/누락 거부·임시 금지 의존 실패; [T24 실행](t24-verification.md) |
| 실제 JAR→웹앱 진입·직접 링크 | Vite base `/app/`, Gradle `static/app/`, 기존 Spring 정적 파일 제공 | `/app/index.html`·JS/CSS·직접 hash 링크·같은 origin API·REST Docs 실제HTTP/브라우저2건; [T24 실행](t24-verification.md) |
| 상세 링크의 일반·modifier 클릭 | StationCard·CandidateGroup의 props/event와 실제 href | 일반 좌클릭만 현재 화면 이동; Ctrl/Cmd/Shift/Alt·가운데 클릭 기본 동작 유지, Red10→대상15통과; [최종 검토](t21-t24-final-review.md) |

T18은 검색조건폼·hash이동·단위/타입/lint/build·기존verify/CI연결을 구현했다. 폼4건·Router2건·진단수집3건과 현재Codex브라우저의 실제조작 근거는 [프런트 기록](frontend-verification.md)에 있다. T23에서 실제 연결 E2E·실패 화면을 확인했다. T24에서 JAR 제공과 품질 검사를 확인했다.

현재 검색 진입점의 Stitch 디자인 적용은 [디자인 대응 기록](frontend-design.md)을 따른다. Form의 native radio/submit과 App의 본문 focus를 실제 unit·Codex 클릭/키보드로 확인했다. 해당 디자인 PR 당시147건/10파일이며 기존 T19 API133건을 유지했다. T20의 현재210건/17파일은 위치·검색 결과를 추가 검증한다. 상세·추천 시안의 연결은 후속 범위다.

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
- [필수 suite 목록](required-test-suites.json): T10~T15를 포함한 기능·구조·성능 데이터 suite34개. 실제 XML에 모두 실행되어야 한다. 고정 총건수·coverage 비율은 강제하지 않는다.
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

## T14 종합 HTTP 사용자 흐름

ChargingJourneyTests3건: 실제 fixture HTTP→XML→수집 commit→H2→검색→상세→첫 ID 제외 추천. 중복·관측 시각 누락·합성 관측의601초 경과·503 중 기존 조회·복구·반경 밖 빈 결과를 확인한다. 외부 경계와 시계만 제어하며 실제 업무 객체는 유지한다. [재현 절차와 범위](demo.md).
공용 verify는 필수 suite와 charging-journey HTTP 증거의 형식/건수를 검사한다. 화면 cmux·실공공 API 인증은 별도 미확인이다.

## T15 성능 실험 경로

`python3 scripts/performance/load.py --output <새폴더>` → test classpath의 실제 서버 → 도메인/Repository 생성 데이터 → HTTP 정확성/SQL 기준선 →20worker 준비30초/측정180초→전후 정확성. PerformanceDatasetTests1건은 작은 규모에서5개 충전기/충전소·4종 상태/시각의 실제 매핑을 확인한다. Python 집계3건은 nearest-rank·빈 측정 거부·HTTP/응답 오류 포함을 확인한다. 실제 전체 규모와 시간은 별도 [성능 기록](performance.md)의 원본 측정 결과로 확인한다. 운영 JAR에는 performance 패키지가 들어가지 않아야 한다.

## T16 조회 후보 축소

SearchBounds → ChargerRepository.findSearchCandidates → 기존 정확한 거리·정렬·추천 정책. StationQueryEfficiencyTests의 SQL2/엔티티2 assertion과 기존 반경·날짜 변경선·양극 테스트로 계약 유지/로딩 축소를 검증한다. 측정 근거는 [T16](t16-verification.md)·[성능](performance.md)이다.

## T17 장애·재시작 경계

기존 recovery/sync/scheduler/journey assertion에 여러 회차 장애/복구 성공시각을 보강했다. `scripts/verify-memory-restart.py`는 별도JVM3개와 fixtureHTTP를 통해 수집완료→메모리소실/준비전→재수집완료를 확인한다. 공용 verify에 통합되어 실패를 거부하며 reliability 테스트 패키지의 운영JAR 포함도 거부한다. [보장·한계](reliability.md).

## T19 프런트 계약 경로

URL query→criteria(empty/valid/invalid)→기능 API→공통 Axios/오류 경계→unknown JSON 구조 검사→타입 응답. criteria36·ID20·apiError14·검색31·상세14·추천18건, 기존9건과 전체142건이다. `[Vue warn]`/console 진단·정리/격리 공용 setup을 그대로 사용한다. 필수9파일과0개/skip/누락은 기존runtime 검사가 확인한다. [Red/Green/책임·한계](frontend-verification.md). 화면의 위치→검색 연결은 T20에서 구현했다.

T19 PR37/main3191b89·필수 CI37232622126 실제 성공. T18/T19 화면 근거와 개발 서버 재시작 전후 진단은 프런트 실행 기록을 따른다. 당시 검색 화면 연결은 후속 범위였으며 아래 T20에서 확인했다. 전체 E2E는 T23 계획이다.

실행 진단 후속 회귀: console mock을 복원해도 교체/진단 이력을 잃지 않도록 테스트용 monitorConsole을 적용했다. 수명 단위2건·실제8probe 거부/복원, 최종 프런트144건/필수10파일. 제품/API 계약은 유지한다. 프런트 실행 기록의 후속 회귀를 따른다.

## T20 위치·실제 주변 검색

폼은 미제출 입력, URL은 확정 조건, Store는 요청 순번·조회 상태·결과, 위치 composable은10초/취소를 소유한다. 서버 판정을 재계산하지 않고 표시 모듈과 배지·카드로 표현한다. 기존필수 목록에 새7파일을 등록했다. 공용verify Java360·Python21·프런트210건·타입/lint/build·JAR 실제HTTP·메모리재시작3회 종료0. 현재 cmux의 실제 합성 공급자→H2→검색/빈 결과/권한 거부 복구·URL/뒤로 가기·375/320px를 assertion했다. [명령·실패·한계](t20-verification.md).

## 운영 CI/CD 경계 (2026-10-12)

```text
main push → 기존 CI 전체 gate → 성공한 CI artifact
          → Production CD → 출처·해시·현재 main 확인
          → 앱 전용 WIF/IAP·forced SSH → VM blue/green 조정자
          → 준비 상태·공개 경로 확인 → 기존 요청 종료 → 이전 슬롯 정지
```

- 진입: `.github/workflows/deploy.yml`. `CD_ENABLED=true`인 경우에만 자동 배포
- 실행물: `scripts/cd_release.py`가 기존 CI artifact와 manifest를 검증. VM에서 재빌드하지 않음
- 자격 증명: 앱별 WIF·SSH 계정 분리. DB·OpenAI 비밀은 VM의 root 전용 환경 파일에 유지
- 실패: 준비 실패는 기존 슬롯 유지. 공개 경로 실패는 기존 경로 복구. drain 미완료는 양쪽 슬롯 보존·다음 배포 거부
- 재시도: 현재 main의 성공한 push CI만 허용. VM의 실행 순서 기준으로 오래된 실행물 거부
- 운영 롤백: VM의 `deploy-service.py --app plugpass rollback`; DB 파괴적 변경을 되돌리는 명령은 아님
- DB 변경: 이전·새 앱이 함께 사용할 수 있는 변경만 먼저 적용. 삭제·타입 축소는 별도 배포 단계 필요
- 활성화·실제 운영 결과: gcp-deployment 저장소의 운영 기록이 소유. workflow 파일 존재를 배포 완료로 간주하지 않음

## T30/T31 지도 영역·주소 검색

```text
지도 moveend → StationSearchView의 미확정 영역 → 이 지역 재검색 → URL
  → stationSearchStore → stationApi → StationController
  → StationQueryService → MapSearchArea·Repository 경계/호환 필터 → 거리·ID순 limit

주소 입력 → placeSearchStore → placeApi → PlaceSearchController
  → PlaceSearchService → KakaoPlaceSearchClient (서버 키, DB 트랜잭션 없음)
  → 최대10개 후보 → 선택 위치 변경 → 사용자 검색 확정
```

- 검색 전에도 지도 열기 가능. 반경 선택 UI는 제거하되 기존 radius API/URL·추천 계약은 유지한다.
- 결과 교체는 지도와 영역을 보존한다. 상세/뒤로가기/새로고침은 URL의 경계를 복원한다.
- 유한 좌표·순서·축1도·중심~모서리10km 상한, 경계 포함·필터 후 limit. 너무 넓으면 확대를 안내한다.
- 주소 Store는 요청 세대/취소와 로딩·성공·빈 결과·오류를 소유한다. 입력 변경/화면 이탈 후 늦은 응답은 버린다.
- 키 없음503, 인증503, 제한429, timeout504, 외부/계약 오류502. 키/공급자 원문 오류는 공개하지 않는다.
- [실행 기록](t30-verification.md), [주소·영역 REST Docs](../src/docs/asciidoc/index.adoc), [다음 어댑터 개선 조사](adapter-ownership-research.md).

## 운영 수집 기반 T33/T34

`--plugpass.ingestion.command.enabled=true` → IngestionCommand → StationSyncService → IngestionLock → 기존 페이지 수집/저장. 웹 자동 수집과 별개인 일회 실행이며 MySQL은 연결 단위 잠금으로 다른 JVM을 거부한다. ALL 지역은 전국 zcode 생략이다. `IngestionCommandTests`, `MySqlDeploymentTests.skipsWhenAnotherConnectionOwnsIngestion`, `PublicDataClientTests.searchesAllRegionsWithoutRegionParameter`와 기존 실패 보존 회귀를 실행한다. [실제 검증·운영 미완료 범위](production-data-verification.md).

## T35 상태 변경분 수집

CLI `--plugpass.ingestion.command.mode=auto` → ScheduledSyncService/CollectionSchedule → 기본정보 또는 StatusSyncService → DB 잠금/공공 상태 API → StatusUpsertService/Charger. 검증: CollectionScheduleTests, IngestionCommandTests, StatusSyncTests, StatusUpsertTests, ChargerStatusChangeTests, CollectionReadinessTests 및 실제 MySQL 배포 계약. [실행·한계](status-sync-verification.md). 수집 상태 화면은 T36, 운영 타이머·백업은 별도 운영 PR4로 전달했다. [운영 실행·머지 기록](production-data-verification.md)을 따른다.

## T36 수집 상태 안내

검색 API → CollectionStatusService/CollectionHealth → collection 필드 → StationSearchView의 준비·지연·성공 시각 안내. CollectionStatusTests, StationSearchHttpTests, StationSearchView.spec.ts, stationApi.spec.ts 및 ui-failures의 모바일 지연→새로고침 복구가 검증한다. [기록](collection-status-verification.md). 수집 정상과 충전기 관측 최신성을 구분한다.

## 모바일 위치 복구 T38

현재 위치 → 환경/권한별 안내 → 재시도 또는 주소 입력. 운영 합성 예시 입력은 제거한다. 소유자: useCurrentLocation composable(요청 수명/오류)·StationSearchView(사용자 행동/포커스). 기존 API/URL/도메인 정책 유지. 실제 Red/Green·전체 검증·실기기 한계: [T38 기록](t38-verification.md).

## 결과 우선 검색 T39

주소 후보 선택→지도 중심/반경 검색→결과 포커스. 검색 후 조건은 접고 수집 상세는 목록 뒤로 옮긴다. 지연/준비 전은 기본 표시한다. 소유자와 실제 실패/검증: [T39 기록](t39-verification.md).

## 카카오 지도·주소 검색 T44

상단 `PlaceSearch` → `placeSearchStore`(상태·취소·순서) → `placeApi` → 카카오 Web SDK services(운영 키) 또는 기존 서버 REST(키 없는 개발 환경). 후보 선택은 `StationSearchView.selectedLocation` 한 곳에 저장되어 `StationMapSession.moveTo`와 충전소 API 좌표가 일치한다. `kakaoPlaceSearch`가 공급자 결과 검증과 오류·중복을 소유한다. [지도 SDK 기록](t44-kakao-map-sdk.md), [주소 검색 기록](t44b-kakao-address-search.md).

## 검색 선행 조건 안내 T45

`PlaceSearch` 후보 선택 또는 `StationMap` 영역 재검색 → `StationSearchView`가 위치·차량 조건을 확인 → 차량 조건이 없으면 주소 검색 아래 안내·차량 선택 버튼 → `SearchForm`의 선택 상태 변경 → 검색 제출 → 기존 URL·`stationSearchStore`·API. 지도 좌표는 차량 선택 전에도 유지한다. `SearchForm`은 차량별 호환 조건 판단을, View는 위치 선택과 다음 행동 안내를 소유한다. [테스트·실행 기록](t45-search-prerequisite-guidance.md).
