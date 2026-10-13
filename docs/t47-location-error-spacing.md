# 위치 오류 안내 간격 T47

## 사용자 흐름과 책임

```text
현재 위치 사용 → 권한 오류 안내 → 현재 위치 다시 시도 | 주소로 검색하기
```

`useCurrentLocation`은 위치 요청과 오류 내용을 소유한다. `StationSearchView`는 오류가 나타났을 때 문구와 두 행동의 순서·초점을 소유한다. 이번 변경은 View의 오류 블록을 문구와 행동 그룹으로 나누고 CSS `gap`을 적용한다. 좁은 폭에서 행동 버튼은 줄바꿈된다. 위치 권한 처리·API·URL 계약은 바꾸지 않는다.

## Red → Green → Refactor

- Red: `CI=1 VITE_KAKAO_JS_KEY=test-js-key bash scripts/frontend-e2e.sh e2e/ui-failures.spec.ts --grep '위치 오류 안내와 대안 버튼'`에서 320px 화면의 오류 문구 아래 간격 기대 `>=8px`, 실제 `0px`로 실패했다. 실패 화면과 trace는 `frontend/test-results/e2e/`에 남았다.
- Green: 오류 블록에 12px 세로 간격, 행동 그룹에 10px 간격을 주었다. 같은 Chromium 검사 1건 통과, 종료 코드 0. 문구 위·아래, 두 버튼 사이, 가로 넘침 없음이 모두 검증됐다.
- Refactor: 별도 로직이나 상태가 없어 추가 추출은 하지 않았다. `location-error`는 오류 메시지의 레이아웃, `location-error-actions`는 행동 버튼의 레이아웃만 담당한다. 기존 버튼 ID·접근성 역할·클릭 동작은 유지했다.

권한 거부가 오류를 표시하는 실패 경로이며, 320px 폭과 줄바꿈이 경계 경로다. 권한 허용과 주소 검색 이동은 기존 E2E에서 확인한다. 외부 API 실패·응답 순서·DB는 이 CSS 변경과 무관하다.

## 전체 검증

현재 cmux 작업공간의 보조 터미널에서 `bash scripts/verify.sh`를 실행했고 종료 코드 0을 확인했다. 프런트 단위 412건, Java 458건, 개발 모드 Chromium 45건, 배포 JAR 브라우저 5건, MySQL 8건과 재시작 후 브라우저 7건이 통과했다. Java·MySQL XML에서 실패·오류·건너뜀은 모두 0건이다. Chromium은 `CI=1`의 headless 모드였고 명령·로그는 cmux에 표시했다. 실제 iOS Safari의 위치 권한 화면은 이번 CSS 검증 범위에 포함되지 않는다.
