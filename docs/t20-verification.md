# T20 실행 기록

기준: main 077cb26. 기존 frontend-plan.md의 T20을 단일 에이전트로 실행한다.

```text
위치 버튼 → 위치 경계(10초·취소·늦은 callback 차단)
폼 미제출 입력 → 검색 제출 → URL 확정 조건 → View → Store → 기존 API → 서버
                                        ← 요청 순번으로 최신 결과만 반영 ←
```

순서: 위치 경계/Store 테스트 Red → 최소 구현 Green → 화면·표시 계약 Red → Green → 책임 검토 → 전체 verify → cmux 실제 검색 → PR/CI/머지.

- 위치: 자동 요청 없음, 성공, 권한 거부, 획득 실패, timeout, 지원 없음, 늦은 응답, 반복 요청, 화면 이탈.
- 조회: 로딩/성공/400/서버 오류, 이전 성공·실패 응답 역전, reset/이탈, 독립 재시도.
- 화면: URL 복원/뒤로 가기, 잘못된 URL 호출 금지, 준비 전/부분/빈 결과, 상한/거리/보고 수/서버 배지, 조회 시각.
- 디자인: 기존 Stitch 색·타이포·native radio를 유지하며 왼쪽 조건·오른쪽 결과로 확장한다. 모바일은 세로로 배치한다.
- 판정: 최신성은 서버 값 그대로 표시한다. 상세 링크/상세 기능은 T21 범위이므로 이번 카드에 미구현 이동을 붙이지 않는다.
- 환경: Java25.0.4.1, Node26.7.0. worktree 상위의 asdf 기본 Node25가 선택되는 설치 오류는 PATH에 기존26.7.0 실행 경로를 명시해 해결한다. 기능 Red로 세지 않는다.
- 리뷰: 프로젝트 계획의 병렬 에이전트 금지를 유지하고 변경 전체를 별도 자체 리뷰한다.

## 첫 실행

기존147건 통과. 위치·Store21건 중17건 assertion Red →21건 Green·전체168건. 화면·표시34건 Red, RequestState3건 Red, 폼 URL 복원1건 Red(기존6건 유지) → 대상132건·전체206건 Green. 초기 idle/cancel 등4건은 stub에서도 통과한 계약이며 Red 건수에 포함하지 않는다.

type-check는 data-action 속성의 strictTemplates 거부와 exactOptionalPropertyTypes에 따른 adapter undefined 복원을 거부했다. 기능 Red가 아니다. 표준 id와 기존 API 테스트의 adapter 명시 확인 패턴으로 수정한다. lint는 경고0 종료0이었다.

표시 책임 리뷰에서 Object prototype 이름(constructor/toString/__proto__)이 서버의 새 코드로 오면 상속된 값을 반환하는 결함을 발견했다. 회귀3건 실제 Red →20건 Green. 이어서 공용 verify는 현재 TS lib에서 Object.hasOwn 미지원으로 종료2였으며 통과로 취급하지 않는다. 문자열 Map으로 상속 이름 조회 자체를 없앤다. TS lib 설정을 넓히지 않으며 기존 의미는 유지한다.

## 최종 결과와 실제 화면

공용 `bash scripts/verify.sh` 종료0: Java360·Python21·프런트210건/17파일, 실패/오류/skip0. strict 타입·lint 경고0·build, JAR 실제 검색200/조건400/상세404·추천200·문서 일치·메모리 H2 재시작3회를 확인했다. [최종 전체 로그](evidence/t20/review-verify.log.gz)·[runtime](evidence/t20/runtime.json)·[재시작](evidence/t20/memory-restart.json). 최초 타입 실패를 포함한 로그도 보존한다.

호출 workspace:5/surface:6을 identify로 확인하고 오른쪽 surface:7에서 명령/로그를 실행했다. 같은 workspace의 cmux 브라우저 surface:13에서 실제 클릭/방향키/이동을 했다. [실행 환경](evidence/t20/session.json). 시각적 포커스가 다른 workspace였던 최초 identify의 focused 값을 호출 대상으로 사용하지 않았다.

`python3 -u docs/evidence/t20/demo-server.py false`는 수집 없는 실제 서버, `true`는 기존 test-classpath의 RestartProbeApplication과 StationSyncService를 사용해 [합성 XML](evidence/t20/provider.xml) 2곳을 loopback HTTP로 수집한다. Service/Repository/Controller/DB를 mock하지 않는다. 이 스크립트는 검증 기록이며 운영 JAR에 포함되지 않는다. 실행 전 공용verify로 build/performance-classpath.txt를 생성한다. 기본 Vite proxy의8080과 프런트5173이 비어 있음을 확인했다. 종료는 직접 시작한 프로세스만 수행했다. 마지막 재현 서버 로그는 T20_DEMO_LOG_DIR로 저장소 밖에 둘 수 있다.

| 실제 브라우저 조작과 assertion | 근거 |
| --- | --- |
| 예시 위치 선택·검색 → 준비 전 안내/카드0개 | [화면](evidence/t20/before-ready.png) |
| 실제 수집/H2 검색 → 카드2개·0m/88m·보고1/0대·UNVERIFIED 배지 | [assertion](evidence/t20/browser-success.json)·[데스크톱](evidence/t20/desktop.png) |
| NACS 입력만 변경 → URL/이전 결과 유지 → 제출 → 정상 빈 결과 | [미제출](evidence/t20/browser-draft.json)·[빈 결과](evidence/t20/browser-empty.json) |
| 뒤로 가기·새로고침 → 확정 조건/목록 재조회 | [뒤로 가기](evidence/t20/browser-back.json) |
| 1500m/limit1 직접 링크·브라우저 reload → 폼/카드1개/상한 복원 | [복원](evidence/t20/browser-restore.json) |
| 부분 URL → 필드 안내·카드0개·조회 버튼 없음 | [오류](evidence/t20/browser-invalid.json) |
| 현재 위치 버튼 → 실제 WKWebView 권한 거부 → 예시 위치 검색 회복 | [권한 거부](evidence/t20/browser-location-denied.json)·[회복](evidence/t20/browser-final.json) |
| native 반경 radio 방향키 → 미제출 입력3000m, URL1000m 유지 | [키보드](evidence/t20/browser-final.json) |
|375/320px 가로 넘침 없음·선택지/버튼44px 이상 | [375](evidence/t20/browser-mobile.json)·[320](evidence/t20/browser-320.json)·[모바일 결과](evidence/t20/mobile-results.png) |

재시작 때 Vite가 백엔드보다 먼저 열려 최초 검색은 proxy ECONNREFUSED/HTTP 실패였다. 준비된 결과 대기는 timeout이었고, 실제 화면의 서버 오류 안내에서 다시 조회를 클릭해 회복했다. [실패 원인](evidence/t20/frontend-true.log.gz)·[수집 SUCCESS processed2](evidence/t20/backend-true.log.gz). 이 실패를 최초 검색 성공으로 표현하지 않는다. 최초 이동 전 console warn/error·error/unhandledrejection 감시를 등록했고 각 저장 JSON의 JS/Vue 진단은0건, native errors 목록도 없음이었다. HTTP 연결 실패 기록은 별도로 보존한다.

## 책임·실패 조건·한계 리뷰

- SearchForm은 미제출 반경/커넥터만 소유한다. View는 위치 선택·URL 확정·이동/이탈을 연결한다. URL의 검증된 조건이 바뀌면 key로 폼을 다시 만들고 API를 다시 조회한다. 이름→실제 효과→배치를 검토해 카드·표시·조회·위치를 각 소유 모듈에 뒀다.
- Store는 요청 상태/결과/조회 시각·순번을 소유한다. AbortController로 불필요한 요청을 취소해도 이미 도착하거나 취소를 따르지 않는 응답이 있을 수 있어 순번도 확인한다. 이전 성공/실패·reset 후 응답이 최신 화면을 덮지 않는7건을 확인했다.
- 위치 composable은 수동 요청·지원 없음·권한 거부/획득 실패·10초 경계·좌표 유효성·이탈/취소를 소유한다. native one-shot 요청을 중단하는 기능은 없으므로 timer를 정리하고 늦은 callback만 무시한다. 9999ms/10000ms 경계와 좌표 범위 밖을14건으로 확인했다.
- 배지·카드는 서버 status/freshness를 따로 표시한다. 표시 모듈은 한국어 문구·직선거리·시각만 변환하며 서버 업무 판정을 옮기지 않았다. 상속 속성 문제가 없는 Map으로 표시 테이블을 정리했다. 추측성 클래스/인터페이스나 공통 fixture 묶음은 추가하지 않았다.
- 400 fields/503/재시도, 준비 전/부분/정상 빈 결과, URL 복원/순서·이탈은 실행한 테스트로 검증했다. 프런트 View 검증은 실제 Axios의 adapter와 위치 경계만 제어한 단위/화면 테스트다. DB 트랜잭션 변경·중복 저장은 이번 변경 대상이 아니며 기존 실제 backend 검증을 유지했다.
- native 위치 성공은 실제 OS 좌표로 확인하지 않았다. 성공/timeout/늦은 callback은 제어된 위치 단위/화면 테스트, 권한 거부/예시 위치 회복은 실제 cmux에서 확인했다. 실제 공공 API 인증, 상세/추천 UI, Playwright 전체 흐름과 운영 JAR 웹앱은 T21~T24 후속 범위다.
- 프로젝트 계획의 에이전트 병렬 실행 금지를 따라 자체 리뷰했다. 별도 작성자의 독립 리뷰와 같다고 주장하지 않는다. 체크의 로컬 근거는 위 결과이며 전달 완료는 해당 PR의 실제 필수 CI/머지 상태로 판정한다.

## 최종 자체 리뷰의 회귀 보완

위치 callback이 성공한 직후 예시 위치를 선택하면 Promise 후속 처리가 아직 실행되지 않은 사이에 취소가 일어날 수 있다. 단순한 callback 순번만으로 이미 resolve된 값의 View 반영을 막을 수 없었다. View14번째 회귀 테스트에서 예시37.5/127 대신 이전38/128이 URL에 들어가는 실제 assertion Red를 확인했다. View가 반영 직전에 위치 경계의 현재 success 상태를 확인하는 최소 변경으로 새 선택을 유지한다. [Red](evidence/t20/location-selection-red.log.gz). 이 보완으로 최종 프런트는210건/17파일, 대상136건이다. 앞209건은 보완 전 실제 관측 기록이다.

대상136건·전체210건과 공용verify가 최종 종료0이었다. [Green](evidence/t20/review-green.log.gz)·[전체210건](evidence/t20/review-verify.log.gz)·[unit JSON](evidence/t20/unit.json.gz). 수정 전209건의 verify-final.log.gz도 보존했다. 미해결 Critical/Important 발견은 없으며 상세 화면은 계획대로 T21에 남긴다.

| Red 실행 명령 | 실제 실패와 Green |
| --- | --- |
| `npm --prefix frontend run test:unit -- src/shared/location src/features/search/stores` | 상태 전이·타이머·오류·순번17건 실패 →21건 통과. [Red](evidence/t20/boundary-red.log.gz)·[Green](evidence/t20/boundary-green.log.gz) |
| `npm --prefix frontend run test:unit -- src/features/search/views src/features/search/components/StationCard.spec.ts src/features/charging-info` | 화면/배지/표시34건 실패 → 최소 연결. [Red](evidence/t20/screen-red.log.gz) |
| `npm --prefix frontend run test:unit -- src/shared/ui/RequestState.spec.ts` | 안내/필드 오류3건 실패 →3건 통과. [Red](evidence/t20/request-state-red.log.gz) |
| `npm --prefix frontend run test:unit -- src/features/search/components/SearchForm.spec.ts` | 1500m/NACS 복원1건 실패 → 기존6건+복원1건 통과. [Red](evidence/t20/form-restore-red.log.gz) |
| `npm --prefix frontend run test:unit -- src/features/charging-info/presentation.spec.ts` | prototype 이름3건 실패 →20건 통과. [Red](evidence/t20/unknown-code-red.log.gz) |
| `npm --prefix frontend run test:unit -- src/features/search/views/StationSearchView.spec.ts` | 성공 직후 새 선택이 덮이는1건 실패 →14건 통과. [Red](evidence/t20/location-selection-red.log.gz) |

자체 리뷰 후의 책임 배치와 공개 API/URL 계약은 유지했다. 폼/Router 기존 호출부는 Pinia/복원 props만 연결했고, 실제 HTTP 필드·DB 컬럼·백엔드 판단은 변경하지 않았다. 최종 브라우저 재현은 아래 전달 단계에서 현재 코드로 다시 확인한다.

최종 코드의 새 개발 서버에서3km/DC_COMBO 직접 이동·reload·새로고침 버튼을 다시 실행했다. 카드2개·폼3km 복원·Vue/JS 진단0건을 [재현 assertion과 소스 hash](evidence/t20/browser-reviewed.json)로 보존했다. [최종 화면](evidence/t20/final.png)을 현재 cmux 브라우저에 유지하며 서버 로그는 /private/tmp/plugpass-t20-live/에 있다.
