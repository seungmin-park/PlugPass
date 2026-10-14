# 핵심 이용 흐름 개편 실행 기록

기준: 사용자 승인 계획, origin/main 0c8f4a0. 별도 worktree PlugPass-visual.

1. 공통 디자인·조건 유지 헤더: PR84 병합, 운영 배포 실패 원인 확인.
2. 조건→지도→목록, 요약 카드·펼침·선택: PR85 필수 CI 성공·병합.
3. 상세 수집 계약·규격·시각·제한 안내·README: 로컬 전체 검증 완료, PR 전달 단계.

공유 경계: 1의 공통 토큰은 2·3 화면에 적용. 2의 카드 요약은 3의 상태 의미를 유지해야 함.
개별 수신 시각으로 지연을 계산하지 않고 서버 collection을 사용한다.
cmux workspace:1000070003 / 검증 surface:1000070011.

## 1. 공통 디자인
- Red: `npm --prefix frontend run test:unit -- src/router/index.spec.ts`, 2 assertion 실패(메뉴명/조건 펼침).
- Green: 전체 Vitest 416/416, type-check/lint 통과. 기존 baseline 414/414.
- 헤더는 검증된 URL query를 링크에 전달하고 검색 화면에서는 native details/summary를 열어 focus. Ctrl/Cmd 등의 기본 링크 동작 유지.
- Pretendard Variable v1.3.9 원본과 OFL을 로컬 asset으로 제공. 일반 본문과 배경 대비를 계산하며 색 외 문구도 유지.
- cmux에서 실제 검색 결과·헤더 클릭 확인. 공용 verify는 KAKAO_JS_KEY 주입 정보 확인 중.

## 2. 모바일 검색
- Red: StationCard/StationSearchView 대상 2건의 assertion 실패 확인.
- Green: 프런트 417건. 지도 SDK 준비 전 로딩과 검색 로딩을 혼동한 공용 테스트를 실제 SDK fake 준비를 기다리도록 보완.
- DOM 순서를 조건→지도→목록으로 변경. 지도 접기·목록 max-height/overflow 제거. 상태 요약과 native button 펼침을 추가. 핀20px, 터치44px 유지.
- headed 개발 E2E에서 버튼 겹침을 재현하고 재검색을 왼쪽 아래로 이동. 320/375/430/1440px 첫 제목 노출·순서·가로 넘침·스크롤 검사 통과. 지도 최소260px은 이전 디자인 제약으로 새170px 지도·첫 결과 노출 계약으로 교체.
- cmux 직접 카드 펼침 확인. 러너의 지도 SDK mock 화면과 실제 cmux SDK 실패 화면을 구분: 직접 브라우저에서 목록·상세 접근은 가능했고 실제 지도는 아직 미확인.
- README 전면 개편은 사용자 추가 요청으로 3단계에 포함.

## 리뷰와 배포 경계 보완

- 헤더 RouterLink 이벤트 순서 회귀를 Red로 재현하고 native anchor에서 라우터 이동을 소유하도록 수정. 417 프런트 테스트와 전체 공용 verify 통과 후 [PR84](https://github.com/seungmin-park/PlugPass/pull/84) 보호된 자동 병합. main `030f956`, CI `38610696527` 성공.
- PR84 CD `38611644933` 실패: 배포 자산 저장소의 1MiB 상한에 2,057,688바이트 단일 글꼴이 걸렸다. 트래픽 전환 전 실패하여 이전 운영 버전을 유지했다.
- `test_font_assets.py`에서 동일 크기 assertion Red를 확인했다. Pretendard v1.3.9 공식 dynamic subset 92개(최대43,920바이트), 공식 unicode-range CSS·OFL을 자체 제공하는 것으로 해결한다. 외부 글꼴 실행 의존성과 글자 누락을 만들지 않는다.
- PR85 리뷰에서 지도 지역 선택 후 차량 선택, 지도 이동 중 유효 조건 제출이 GPS로 전환되는 결함을 재현했다. `mapSelectionActive`가 위치 선택 의도를 소유하고 지도 조작은 진행 중 GPS를 취소한다. 휠·키보드 버튼 click도 각각 Red 확인 후 반영했다. 늦은 GPS callback이 결과를 바꾸지 않는 assertion도 포함한다.
- 검증 명령: `npm --prefix frontend run test:unit -- src/features/search/views/MapSearchView.spec.ts` (최종7/7). Red 로그 `/tmp/plugpass-map-intent-red.log`, `/tmp/plugpass-map-wheel-red.log`; 폰트 Red `/tmp/plugpass-font-red.log`.
- 불변식: 지도 자동 초기화는 사용자 위치 선택이 아니다. 주소/GPS를 명시적으로 선택하면 지도 선택 상태를 해제한다. 지도 이동 중 영역이 null이면 준비 안내를 유지하고 GPS로 전환하지 않는다.

## 3. 상태 맥락과 프로젝트 소개

- 상세 HTTP의 collection 누락3건, 규격·원본 숨김·수집 상태 표현·카드 선택·조건 요약을 먼저 assertion Red로 확인했다. 로그 `/tmp/plugpass-status-red-java.log`, `/tmp/plugpass-status-red-ui.log`, `/tmp/plugpass-status-red-context.log`.
- Green: DTO에 기존 규격 해석·전체 수집 상태를 추가하고 화면은 공통 CollectionSummary/ChargerStatusSummary를 사용한다. 추천 상태 배열은 호환 충전기의 실제 상태만 포함하며 AVAILABLE/OCCUPIED 혼합+비호환 제외를 정확한 목록으로 검증한다. 새 필드 미제공·알 수 없는 값은 확인 필요로 표현한다.
- 정상 수집+오래된 개별 수신 시각, 전체 지연, 준비 전, 제한·삭제·알 수 없는 규격을 구분한다. 공급자 상태 변경 시각의 시간대는 추정하지 않는다. 외부 HTTP 실패·GPS 거부·지도 SDK 실패·응답 역전은 기존 회귀 suite를 유지한다.
- Refactor: 반복되는 상태 요약/수집 안내를 표현 컴포넌트로 추출하고, 중복·덮어쓰기 CSS를 정리했다. View가 위치/라우팅·선택을 소유하고 DTO는 응답 변환, 기존 도메인은 규격 판단을 소유한다. 새 저장 구조나 트랜잭션 변경은 없다.
- 전체 E2E에서 새 문구에 맞지 않는 assertion과 데스크톱 첫 제목 밀림을 확인했다. 문구 계약을 갱신하고 지도240px·수집 안내 줄 배치를 조정했다. 320/375/430/1440px 첫 제목·겹침·가로 넘침·44px 터치·키보드·200% 글자 확대를 다시 검사한다.
- README를 제품 소개·사용 흐름·상태 한계·기술 역할·로컬 실행·검증 안내 중심으로 다시 작성했다. 상대 링크 검사 누락0.
- HTTP 배열 검증 보강 중 JsonPath의 실제 배열 타입을 List 구현 클래스로 변환하는 assertion이 null을 반환했다. 응답 원문 배열은 정상임을 확인하고 Hamcrest 전체 배열 동등성으로 수정했다. 기능 Red로 계산하지 않았다.
- 현재 대상/프런트 검증: Java 상세10·추천 도메인 통과, 프런트431·type-check·lint 통과. 최종 전체 verify와 원격 CI/배포 결과는 아래에 추가한다.
- PR85 최종 공용 verify 종료0: Java458·프런트422·개발 E2E56·JAR5·MySQL 계약8/브라우저7. `/tmp/plugpass-pr2-final-verify.log`.

### 최종 검증 중 발견한 경계

- 상세에 collection이 추가되면서 MySQL 재시작 전체 응답 비교가 실패했다. 테스트 수집 시계(2026-10-05)와 운영 JAR 실제 시계의 차이로 검색·상세 모두 CURRENT→DELAYED가 되는 정상 계산이다. 목록과 상세 collection 전체 일치를 먼저 assertion하고, state를 제외한 모든 저장 필드의 재시작 전후 동등성은 유지했다. `/tmp/plugpass-stage3-final-verify.log`의 실패를 통과로 계산하지 않았다.
- 실제 로컬 SDK 요청은 localhost:5183과 127.0.0.1:5183에서 HTTP401, 운영 도메인 Origin에서는 HTTP200이었다. 로컬 도메인 허용 범위 때문에 cmux의 실제 지도는 실패 화면이다. 자동 E2E는 SDK mock을 사용한다. cmux에서는 카드 펼침→상세→대체→헤더 복귀와 query 유지를 직접 확인했다. 실제 타일·지역 검색은 운영 배포 후 확인한다.
- 로컬 최종 화면 캡처: `frontend/test-results/redesign-{320,375,430,1440}.png`, `redesign-large-text.png`; 직접 브라우저 상세 `/tmp/plugpass-final-local-detail.png`. 운영 전 기준 `/tmp/plugpass-production-before.png`. 데이터가 서로 다르므로 픽셀 동일성 대신 위계·첫 제목·겹침·조건 복귀를 비교한다.
- iPhone Safari 실기기 지도 터치·글자 확대·앱 전환은 이 환경에서 실행하지 않았으며 Chromium의 모바일 크기 검증으로 대체 완료 처리하지 않는다.

### 최종 로컬 검증 결과

`/tmp/plugpass-stage3-final2-verify.log`, 현재 cmux 검증 pane에서 `VERIFY_EXIT:0` 확인. Java462·프런트431·개발 headed E2E57·JAR 브라우저5·MySQL 계약8/브라우저7, 실패·오류·skip0. type-check/lint/build 통과. MySQL 재시작 뒤 저장 데이터·수집 이력 유지 및 시간에 따른 상세/목록 지연 상태 일치 통과.

320/375/430/1440px 순서·첫 제목·겹침·넘침, 200% 글자 확대, 키보드 선택/닫기, 핀44px·표시20px을 확인했다. 대비: 본문13.98:1, 보조 글자5.99:1, 흰 글자/청록 버튼6.15:1, 흰 배경 상태 문구8.30/6.64/7.97/7.50:1. 원격 CI·운영 배포는 이 로컬 결과와 별도로 기록한다.


### PR 전달 근거

- [PR84](https://github.com/seungmin-park/PlugPass/pull/84): CI 성공·보호된 자동 병합 `030f956`. 첫 CD는 글꼴 크기 제한으로 실패했으며 PR85에 수정 포함.
- [PR85](https://github.com/seungmin-park/PlugPass/pull/85): 최종 head `720a76b`, CI `38612594674` 성공, 보호된 자동 병합 `2e9e29b`. 이후 main CI와 Production CD가 운영 전달을 검증한다.
- 3단계는 최신 main `2e9e29b`를 병합했다. squash 이력으로 생긴 중복 충돌을 해결한 뒤 전체 파일 tree `28e269e2a02d60aab24c51ea3abfc00c62dbd922`가 로컬 검증한 구현과 같음을 확인했다. 이후 변경은 작업 상태 문서뿐이다.
- 최종 원격 CI·CD·운영 지역별 화면 확인 결과는 PR 본문/Actions 링크에 후속 기록한다. 이 문서의 로컬 통과를 배포 성공으로 해석하지 않는다.
