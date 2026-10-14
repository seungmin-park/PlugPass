# T52 지도와 결과 경계 검증

사용자 제보: iPhone Safari의 지도 아래쪽에 `주변 충전소` 패널이 겹쳐 보인다. 기존 CSS는 결과 컨테이너에 `margin-top: -18px`를 주어 지도 위로 올렸고, 기존 브라우저 테스트도 8px 이상 겹침을 요구했다.

검증 기준: 320px·375px 화면에서 지도 아래와 결과 패널 위 사이가 최소 8px, 첫 카드 상단은 620px 미만, 가로 넘침 없음. 지도 실패·GPS 거부·직접 URL 흐름은 유지한다.

Red: `CI=1 VITE_KAKAO_JS_KEY=test-js-key KAKAO_JS_KEY=test-js-key bash scripts/frontend-e2e.sh e2e/nearby-first.spec.ts --project=chromium`에서 두 해상도 모두 겹침이 18px로 새 assertion에 실패했다. 개발 서버만 띄운 첫 실행은 백엔드 502였으므로 기능 Red로 세지 않았다. SDK 키가 없는 재실행 역시 지도 높이 0인 환경 실패로 분류하고, 공용 E2E 서버·SDK 모의 키로 다시 실행했다.

Green: 결과 컨테이너를 지도 아래로 8px 내리고, 목록 내부 위 여백을 8px 줄였다. 첫 로컬 대상 브라우저 테스트 4건은 통과했지만 PR CI에서는 폰트·렌더링 차이로 첫 카드가 631px에 놓여 620px 기준에 실패했다. 결과 노출을 위한 지도 높이를 16px 조정하되 최소 280px을 유지했다. 수정 뒤 대상 브라우저 테스트 4건이 다시 통과했다. 지도·검색 상태 소유자와 API·URL은 바꾸지 않았다.

CI 차이를 반영한 최종 공용 `CI=1 KAKAO_JS_KEY=test-js-key bash scripts/verify.sh`는 현재 cmux 보조 pane에서 종료 코드 0으로 끝났다. 프런트 단위 421건, 개발 Chromium 57건, 배포 JAR Chromium 5건, 실제 MySQL 8건, 재시작 뒤 Chromium 7건이 통과했다. 로그는 `/tmp/plugpass-map-overlap-verify2.log`다. PR CI·병합 결과는 별도로 확인한다. 실제 iPhone Safari의 CSS 배치는 사용자가 재확인할 수 있으며, 로컬 Chromium 결과를 iOS 실기기 실행으로 대신하지 않는다.
