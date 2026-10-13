# T28 길찾기·T29 처리 중 표시 검증

2026-10-13. 사용자 요청으로 plan.md·tasks.md에 T28을 추가했다. 현재 상세 API의 이름·좌표를 사용하며 API/DB 계약은 바꾸지 않는다. 카카오맵과 네이버 지도로 자동차 길찾기를 열고, 미설치/실행 차단에는 설치·웹 링크를 제공한다.

```text
StationDetailStore: 현재 조회·취소·응답 순서·초기화
  → StationDetailView: 상세 페이지 조립
  → StationDirections: 길찾기 펼침·앱/설치/웹 선택
  → navigationLinks: 목적지 검증·인코딩·OS별 URL
  → OS/외부 지도 앱: 실행·현재 위치·경로 계산
```

## 실제 Red → Green

1. `npm --prefix frontend run test:unit -- src/features/station-detail/views/StationNavigation.spec.ts`.
   첫 실행의 jsdom `maxTouchPoints` 부재는 테스트 준비 오류였다. navigator의 프로토타입을 유지하는 환경 대체로 수정했다. 재실행은 **12건 중8건 assertion 실패·4건 기존 입력 거부 통과**, 종료1. 길찾기 summary/앱 링크가 없어서 `expected false to be true`. 근거 `/tmp/plugpass-t28/red-corrected.log`.
2. 순수 링크 모듈과 표현 컴포넌트를 추가하고 기존 상세에 연결했다. 같은 API/Store를 사용하는 상세 관련5파일45건 통과. `/tmp/plugpass-t28/green-detail.log`.
3. `npm --prefix frontend run test:unit -- src/features/station-detail/navigation/navigationLinks.spec.ts`:19건 중16건 실패·3건 통과. 잘못된 이름/좌표에도 URL 생성, 네이버 지원 범위 밖 링크 제공을 assertion했다. `/tmp/plugpass-t28/red-boundary.log`.
4. 기존 공통 validation을 재사용하고 네이버의 공식 좌표 범위를 적용했다. 전체29파일321건 통과. `/tmp/plugpass-t28/green-unit.log`.
5. 타입 검사에서 strictTemplates가 임의 data 속성3개(두 빌드 대상에 중복 보고)와 adapter 복원 undefined1개를 거부했다. DOM 식별은 기본 id 속성, adapter는 기존 테스트처럼 정의 여부를 명시 확인했다. 엄격한 검사를 완화하지 않았다. 수정 후 타입/lint/build가 통과했다.

Refactor: import 배치를 정리했다. 새로운 Store나 Service는 필요하지 않아 만들지 않았다. 링크 생성은 순수 함수이며 설치 조회·타이머·재시도 상태가 없다. 기본 details의 펼침 상태는 컴포넌트 수명에 속하고 다음 상세 조회 때 제거된다. 숫자 기준을 맞추는 래퍼/인터페이스는 추가하지 않았다.

## T29 회전 로딩 표시

작업 중 사용자 추가 요청으로 T29를 계획/체크리스트에 추가했다. 기존 비동기 상태를 공용 `LoadingIndicator(message)`에 연결해 원형 CSS 회전과 작업 문구를 함께 표시한다. 위치·검색·상세·후보·수집 상태·지도의 기존 상태 소유권과 timeout/취소 규칙을 유지한다. 새 진행률이나 비동기 Store를 만들지 않는다.

RED: `npm --prefix frontend run test:unit -- src/shared/ui/RequestState.spec.ts src/shared/ui/LoadingFlows.spec.ts src/features/search/components/StationMap.spec.ts`에서16건 중9건 assertion 실패·7건 기존 동작 통과. `/tmp/plugpass-t28/loading-red.log`. spinner 부재와 실제 조회 중 표시 영역의 빈 배열이 실패 원인이다.

GREEN: 공용 컴포넌트와 여섯 상태 표시 지점에 연결한 뒤 전체30파일328건·타입/lint/build 통과. `/tmp/plugpass-t28/loading-green.log`. idle/success/error 뒤 제거·재조회 시 재등장, 실제 API 경계 지연을 통한 검색/상세/후보+수집 상태 표시, 위치 취소·지도 SDK 대기를 검증했다. 외부 I/O와 CSS 표현을 제외한 상태 규칙은 대체하지 않았다.

`loading.spec.ts`는 실제 백엔드 요청을 잠시 보류한 뒤 재개하여 회전 transform의 시간 변화·반복 애니메이션·375px 넘침 없음·응답 뒤 제거를 검증한다. 감소 설정에서 animationName=none과 문구 유지,503 오류 뒤 제거·실제 재시도 성공을 별도로 확인한다. 기존 timeout E2E에도 spinner 제거 assertion을 추가했다. 이 spec도 개발/운영 JAR 양쪽에서 실행한다.

접근성은 `role=status`, `aria-live=polite`, `aria-atomic=true`로 작업 문구를 전달하고 장식 원에는 aria-hidden을 둔다. `prefers-reduced-motion: reduce`는 회전만 멈춘다. 이 테스트가 실제 스크린리더 음성 출력을 검증한 것은 아니다. Refactor는 공통 표현만 추출해 중복 CSS와 문구 표시 구조를 줄였으며 API·Store·기존 오류 focus를 유지했다.

## 자동 검증

- 새 화면/순수 경계 테스트31건을 기존 필수 프런트 목록에 등록했다. `navigation.spec.ts`3건을 개발 및 MySQL/JAR 검사에 등록하고 필수 실행 파일 누락을 기존 검사기로 거부한다.
- `bash scripts/frontend-e2e.sh navigation.spec.ts`: headed Chromium3건 통과. 실제 합성 공급자 HTTP→수집→H2→상세 API로 목적지를 가져온다. 모바일 UA/375px·사용자 클릭의 전달 URL·웹 새 탭·검색 복귀/다른 충전소·키보드 펼침을 assertion한다.
- T28 단독 및 main 동기화 후 `bash scripts/verify.sh`: 모두 종료0. Java372·Python32·프런트321·개발브라우저24·빈 DB JAR브라우저3·MySQL7·재시작 MySQL/JAR브라우저4건, 실패/오류/skip0. MySQL8.4.11. T29 포함 최종 공용 verify도 종료0: Java372·Python32·프런트328/30파일·개발브라우저27·빈 DB JAR브라우저3·MySQL7·재시작 MySQL/JAR브라우저7건. 실패/오류/skip0. `/tmp/plugpass-t28/verify-loading-final.log`와 [소스 해시·결과 요약](evidence/t28/local-summary.json)에 연결한다.
- 실행 원본: `/tmp/plugpass-t28/verify.log`, `build/verification/runtime.json`, `build/verification/mysql/`, `build/test-results/{test,mysqlTest}/`, `frontend/test-results/{unit,e2e,packaged,mysql}.json`, `frontend/playwright-report/`.

## 성공·실패·경계와 책임 검토

- 성공: iPhone/iPad/터치 Macintosh(iPadOS)·Android·desktop의 목적지와 OS별 연결 규격.
- 입력 경계: 이름 공백·한글/쉼표/URL 구분자·NaN/무한대·위경도 범위/경계, 네이버 지원 범위 안팎. 호출자 appname에는 검색 조건·hash가 없다.
- 실패/순서: 깨진 상세·네트워크 오류는 링크 없음, 재시도 성공 후 표시, 다음 상세 로딩 중 이전 링크 제거·새 목적지 반영. 기존 Store의 응답 역전 테스트를 유지한다.
- 외부 경계: 명시적인 앱 설치/웹 대안, Android intent fallback URL. 실제 설치 감지나 OS 실행 성공은 추정하지 않는다. 백엔드 데이터 변경·트랜잭션·추가 API·네트워크 재시도는 이 링크 기능에 해당하지 않는다.
- 이름/책임/배치: `createNavigationLinks`는 조회/저장 없이 링크 생성, `navigationPlatform`은 환경 힌트 판별, `StationDirections`는 props 기반 표현. API 모듈→화면 역의존이 없고 기존 JSON·DB·라우트 계약을 유지한다. 외부 지도 URL은 각 앱의 공식 계약으로 검증한다. TypeScript 모듈은 단일 책임과 명시적 입력을 유지하며 Vue는 props/렌더링 경계로 평가했다.

## 현재 cmux와 실기기 구분

호출 환경의 CMUX_SURFACE_ID와 `cmux identify --json`의 caller를 대조했다. workspace:1000050003 / 호출 surface:1000050003, 검증용 오른쪽 pane surface:1000050012, 브라우저 surface:1000050013. 현재 workspace에 보조 pane이 없어 focus false로 생성했다. 작업 경로는 `/Users/seungmin/Desktop/repo/PlugPass`다. 모든 로컬 러너는 이 전용 pane에서 headed로 실행한다. 브라우저 surface 명령에는 CLI가 workspace 옵션을 받지 않아 명시적인 surface를 사용하며 workspace는 생성 시 확정했다.

cmux에서 예시 위치 선택→주변 검색→가까운 충전소 상세→길찾기를 실제 클릭했다. 데스크톱 웹 목적지 `37.5,127`을 확인했다. iPhone UA와375×812로 다시 열어 두 앱/설치/웹 대안을 표시하고 네이버 링크를 실제 클릭했다. 외부 실행 직전 한 번의 클릭만 가로채 `nmap://route/car?dlat=37.5&dlng=127` 전달, 펼침 상태, 가로 넘침 없음 assertion PASS를 확인했다. 이는 설치 앱 실행 성공을 주장하는 검증이 아니다. cmux WKWebView의 수동 흐름과 Chromium의 외부 URL 경계 interception은 다르다. 모바일 UA/너비는 실기기가 아니다.

T29도 같은 cmux 브라우저에서 예시 위치→검색을 실제 클릭했다. 다음 실제 XHR 한 번만 7초 지연시키는 브라우저 검증 경계를 두고 자동 복원했다. 제품 loading 상태를 직접 변경하지 않았다. 375px에서 진행 문구·회전 transform·가로 넘침 없음, 실제 응답 후 2개 표시·spinner 제거 assertion PASS를 확인했다. console에는 Vite debug만 있었고 browser error는 없었다. [길찾기 화면](evidence/t28/mobile-directions.png), [검색 중 화면](evidence/t28/mobile-loading.png)을 보관했다. 직접 시작한 개발 서버는 종료하고 검증 pane은 유지했다.

실기기 Android Chrome/iPhone Safari에서 (1) 앱 설치 상태에서 정확한 목적지, (2) 앱 미설치와 설치/웹 대안, (3) 실행 취소/앱 복귀 시 뒤늦은 이동 없음, (4) 위치 권한 거부와 출발지 확인, (5) 인앱 브라우저 차단 후 외부 브라우저 재시도를 확인해야 한다. 연결된 실기기가 없어 **미확인**이며 tasks.md에서도 미완료다.

## 전달 상태

원격 main `05467ed`의 배포 자동화 변경을 충돌 없이 반영했다. 2026-10-13 조회 기준 main은 최신성 필수/관리자 포함 보호, GitHub Actions15368의 `PlugPass verify` 필수, 승인0명, 강제 push/삭제 금지, native auto/squash 가능이다.

다만 `CD_ENABLED=true`이고 production environment 승인 규칙이 없어 main CI 성공이 운영 배포로 이어진다. AGENTS.md가 기능 전달 권한에 배포를 포함하지 않으므로 PR·필수 CI까지 진행하고 자동 머지 신청은 운영 배포 확인 이후로 남긴다. 배포 설정을 임의로 바꾸지 않는다. PR·최종 CI는 생성/실행 후 기록한다.
