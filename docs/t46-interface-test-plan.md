# T46 지도 축척·화면 간격 검증 계획

범위: 지도 안의 확대·축소(축척 변경), 검색 전/후 지도, 주소 검색, 검색 조건, 결과 카드, 충전소 상세, 대체 후보의 화면 간격과 정렬. 담당: 구현·검증 담당자 1명. 지도 창 높이 조절은 범위가 아니다. API·DB는 변경하지 않는다.

| 요구 | 확인할 동작 | 자동 검증 | 화면 검토 | 우선순위 |
| --- | --- | --- | --- | --- |
| R1 지도 확대·축소 | 모바일·데스크톱에서 버튼, 터치·휠, 키보드로 확대 수준을 바꾸고 한계를 지킨다 | `StationMap.spec.ts`, `station-map.spec.ts` | 320/375/768/1440px | 높음/중간 노력 |
| R2 지도 좌표 유지 | 확대·축소 뒤 현재 영역 재계산, 마커·재검색 유지 | `kakaoStationMap.spec.ts`, `station-map.spec.ts` | 검색 전/후 지도 | 높음/중간 노력 |
| R3 지도 안내 배치 | 접기 버튼·안내·지도·재검색 버튼 사이 간격을 구분하고 겹침을 막는다 | `station-map.spec.ts`의 위치·가시성 assertion | 320/375/1440px | 높음/낮은 노력 |
| R4 주요 화면 정렬 | 주소 검색, 검색 조건, 결과 카드, 상세·대체 후보의 버튼과 설명이 접하지 않고 가로 넘침이 없다 | 기존 `webapp-quality.spec.ts`, 신규 화면 배치 assertion | 정상·빈 결과·오류 화면 | 중간/중간 노력 |
| R5 실패·접근성 | SDK 실패 시 목록 유지, 조절기 이름·범위·초점, 작은 화면에서도 44px 조작 영역 유지 | 기존 지도 실패 E2E + 신규 조절 테스트 | 키보드·320px | 높음/낮은 노력 |

실행 순서: 기존 테스트와 화면 기준 확인 → 실패 assertion 작성·실행(RED) → 최소 구현(GREEN) → 전체 `bash scripts/verify.sh` → 현재 cmux 보조 pane에서 Chromium headless E2E 로그·스크린샷 확인 → CI 확인. 카카오 지도의 확대 수준이 작을수록 확대되는 계약은 [공식 확대 수준 예제](https://apis.map.kakao.com/web/sample/changeLevel/)를 기준으로 한다.

검증 시간 상한은 자동 테스트·수정 4시간, 화면 폭/상태 점검 1시간, 실패 재현·재검증 여유 1.5시간으로 잡는다. 시간 추정은 일정 약속이 아니라 우선순위 판단용이다. 시작 조건은 현재 main의 관련 테스트 실행 가능, 종료 조건은 R1~R5의 실행 근거와 열린 중대 결함 없음이다. 실제 결과와 남은 차이는 이 문서에 갱신한다.

## 실행 결과 (2026-10-14)

- RED: `npm run test:unit -- --run src/features/search/components/StationMap.spec.ts src/features/search/map/kakaoStationMap.spec.ts`에서 확대 조작 부재와 `zoomIn` 부재로 2건 실패했다. 키보드 확대 후 상태 동기화 부재도 `StationMap.spec.ts`에서 별도 실패를 확인했다.
- GREEN: `StationMapSession`이 SDK 확대 수준을 읽고 변경하며, `StationMap`이 버튼·상태 안내를 표시한다. 카카오 확대 이벤트를 받아 터치·휠·키보드 변경도 동기화한다. 지도와 재검색 버튼의 겹침을 없애고 검색·후보 화면 간격을 정리했다.
- 모바일 375px과 데스크톱 1440px의 검색 전·결과·상세·대체 후보를 headless Chromium 스크린샷으로 확인했다. 320·375·768·1440px 지도 상하 간격과 가로 넘침은 정식 E2E assertion으로 확인했다. 스크린샷은 `/tmp/plugpass-t46-{initial,results,detail,alternative}-{375,1440}.png`에 있다. SDK 지도 배경은 E2E mock이며 실제 카카오 타일의 시각 검증은 이 캡처로 주장하지 않는다.
- 첫 전체 실행의 지도 간격 E2E는 320px 전환 중 레이아웃을 따로 읽어 이전·새 높이를 섞은 계측 오류로 실패했다. 실패 화면은 실제 겹침이 없었고, 한 브라우저 평가에서 모든 요소를 측정하도록 고친 뒤 지도 E2E 7건과 전체 검증을 재실행했다.
- 최종 `bash scripts/verify.sh` 종료0. `build/verification/runtime.json`: Java458·프런트412·개발 Chromium44·패키지 브라우저5, 실패/오류/skip 0. 실제 MySQL8·재시작 JAR 브라우저7도 통과했다. 실행 로그: `/tmp/plugpass-t46-verify-final.log`. SDK 다운로드·타일 실패, 빈 결과·서버 오류는 기존 E2E에서 통과했다. iPhone Safari 실기기 조작은 이 Chromium 검증의 범위 밖이다.
- 이름·책임 검토: `zoomIn/zoomOut/getZoomLevel`은 지도 어댑터가 소유하고 View는 이를 호출해 상태만 보여준다. `StationSearchView`의 지역 재검색과 API·URL·DB 계약은 변경하지 않았다. 임시 화면 캡처 테스트는 삭제하고 실제 사용자 경로 assertion만 유지했다.
