# T51 지도 카드 행동 정리 검증 계획

## 사용자 흐름

```text
검색 결과 카드 ── 길찾기 ──► 선택 패널
              └─ 상세 보기 ─► 상세
지도 마커 ── 선택 ──► 같은 카드 강조 + 선택 패널
```

`지도에서 선택` 버튼은 카드의 길찾기와 같은 패널을 여는 중복 행동이라 제거한다. 지도 마커 선택의 맥락은 카드 강조로만 표시한다. `StationSearchView`가 선택 ID를 소유하고 `StationCard`는 선택된 시각 상태와 두 사용자 행동만 표시한다. 지도 접기 버튼은 지도 왼쪽 아래에 두어 첫 마커를 가리지 않으면서 첫 결과를 아래로 밀지 않는다.

| 상황 | 기대 결과 | 테스트 |
| --- | --- | --- |
| 지도 열림·카드 | 중복 버튼 없이 길찾기·상세 보기만 표시 | `StationCard.spec.ts`, `station-map.spec.ts` |
| 첫 마커 탭 | 접기 버튼에 가리지 않고 해당 패널·카드 강조 | `station-map.spec.ts`, `StationMapSelection.spec.ts` |
| 다른 마커·재검색·새로고침 | 강조 대상 변경·이전 선택 초기화 | `StationMapSelection.spec.ts`, `station-map.spec.ts` |
| 지도 SDK 또는 배경 실패 | 목록 길찾기·상세 보기 사용 가능 | `StationMapAvailability.spec.ts`, 기존 브라우저 실패 테스트 |
| 모바일/키보드 | 결과 목록 위치·가로 폭·초점 복귀 유지 | `nearby-first.spec.ts`, `station-map.spec.ts` |
| 국내 여러 거점 | 서울·부산·대전·대구·광주·제주 좌표로 API 요청하고 결과 카드와 지도 마커를 연결 | `domestic-hubs.spec.ts` |

## TDD·실행 기록

Red: `npm run test:unit -- src/features/search/components/StationCard.spec.ts`에서 중복 버튼 제거 assertion이 실패했다. 현재 cmux 보조 pane의 375px Chromium에서도 첫 마커와 접기 버튼이 겹쳐 별도 위치 assertion이 실패했다. Green: 카드의 `select` 이벤트와 `mapSelectable` prop을 제거했다. 지도 접기 버튼을 별도 상단 줄로 옮기는 첫 구현은 기존 결과 위치·확대 버튼 배치 검사가 실패해, 지도 왼쪽 아래 배치로 조정했다. 대상 단위 24건 통과. 선택 ID는 View에 남기고 카드의 `selected` prop은 시각 강조에만 사용한다. 서버 검색·URL 계약은 유지한다.

E2E 가짜 카카오 SDK가 지도 왼쪽 위에 빈 확대·축소 버튼 두 개를 그려 테스트 화면에만 나타났다. 가짜 버튼을 제거하고 브라우저 테스트가 실제 PlugPass 확대 버튼을 사용하도록 바꿨다. 주소·지도 흐름 대상 Chromium 10건은 종료 코드 0으로 통과했다. 실제 운영 지도에는 해당 가짜 버튼이 없으며 카카오 지도 타일 10개가 HTTP 200으로 로드되는 것도 확인했다.

운영 API를 읽기 전용으로 확인한 2026-10-14 기준, 시청 주변 1km·DC 콤보 검색은 모두 HTTP 200, `dataReady=true`, 수집 상태 `CURRENT`였다. 6곳 모두 375px headless Chromium 운영 화면에서 결과 카드와 지도 핀이 표시됐다. 지도 핀 수는 화면에 렌더링된 수이며 전체 검색 결과 수와 같다는 계약으로 해석하지 않는다.

| 거점 | 운영 검색 결과 | 운영 화면 첫 결과 |
| --- | ---: | --- |
| 서울 | 20곳 | 서울시 본관청사 |
| 부산 | 20곳 | 부산 시청 E-pit |
| 대전 | 20곳 | 대전광역시청 |
| 대구 | 16곳 | 대구시청 |
| 광주 | 20곳 | 삼성HW(Test) |
| 제주 | 20곳 | 제주시청 |

운영 화면 검사는 현재 cmux 보조 pane에서 headless Chromium으로 실행했고 종료 코드 0이었다. 로그는 `/tmp/plugpass-live-hubs-browser.log`, 서울 지도 타일 확인은 `/tmp/plugpass-live-map-debug.log`다. 운영 데이터 건수는 수집 결과에 따라 달라질 수 있으므로 CI의 고정 assertion으로 사용하지 않는다. CI에서는 여섯 거점의 좌표 전달·카드·마커 선택을 독립 fixture로 검증한다.

전체 `CI=1 KAKAO_JS_KEY=test-js-key bash scripts/verify.sh`는 현재 cmux `workspace:1000060002`의 보조 터미널 `surface:1000060010`에서 종료 코드 0으로 끝났다. 프런트 단위 421건, Java 458건, 개발 Chromium 57건, 배포 JAR Chromium 5건, 실제 MySQL 8건과 재시작 후 Chromium 7건이 통과했다. 실패·오류·건너뜀은 0건이다. 로그는 `/tmp/plugpass-t51-verify-complete.log`다. Chromium은 사용자 요청에 따라 headless로 실행했고 러너 명령과 결과는 cmux pane에서 보이게 했다.

CI·병합 결과는 PR에서 확인한다.
