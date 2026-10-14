# T50 상태 색상·지도 핀 검증 계획

## 사용자 흐름

```text
충전기 상태 → 문구 + 고유 배지 색 → 목록에서 빠른 구분
충전소 지도 마커 → 작은 시각 핀 + 44px 버튼 → 선택 패널
```

색만으로 상태를 전달하지 않고 기존의 `이용 가능 보고`, `사용 중`, `이용 불가`, `상태 확인 불가` 문구를 유지한다. 새 공급자 상태는 확인 불가로 보수적으로 표시한다. 작은 핀은 확대 수준과 결과 영역을 덜 가리도록 시각 크기를 줄이고, 누를 수 있는 영역은 줄이지 않는다.

## 확인할 사례

| 조건 | 기대 결과 | 테스트 |
| --- | --- | --- |
| 이용 가능·사용 중·이용 불가·확인 불가 | 각각 구별되는 배지 클래스와 문구 | `ChargingBadges.spec.ts` |
| 알 수 없는 상태 | 확인 불가 문구·색 | `ChargingBadges.spec.ts` |
| 지도 마커 | 시각 핀 24px, 버튼 44px 이상 | `station-map.spec.ts` |
| 가려지지 않은 핀 선택 | 하단 충전소 패널 표시, 선택 상태 유지 | `station-map.spec.ts`, 기존 지도 단위 테스트 |
| 좁은 화면·키보드 | 가로 넘침 없음, 마커 초점 및 Escape 복귀 | 기존 `station-map.spec.ts` |

색상 대비는 브라우저 계산 색상과 화면 검사로 확인한다. GPS·외부 SDK 실패는 기존 검색·지도 테스트가 맡으며 서버/DB 계약 변경은 없다.

## TDD·실행 기록

Red: `npm run test:unit -- src/features/charging-info/components/ChargingBadges.spec.ts`에서 5개 상태의 새 스타일 assertion이 모두 실패했다. Green: `StatusBadge`가 상태에 따라 클래스만 선택하고, 텍스트는 기존 `statusLabel`을 계속 사용한다. 지도 마커는 CSS 가상 요소로 시각 핀과 터치 영역을 분리한다. 대상 단위 13건 통과.

대상 Chromium 검증은 처음에 `VITE_KAKAO_JS_KEY`를 전달하지 않아 지도 SDK 준비가 실패했다. 환경 변수를 바로잡자 첫 번째 핀의 터치 영역이 지도 접기 버튼에 가려진 문제가 드러났다. 보이는 두 번째 핀을 선택하는 대상 검증은 종료 코드 0으로 통과했다. 지도 접기 버튼 겹침과 중복된 카드의 `지도에서 선택` 행동은 다음 지도 조작 UX 작업에서 해결한다.

현재 cmux 작업 공간 `workspace:1000060002`의 보조 터미널 `surface:1000060010`에서 `CI=1 KAKAO_JS_KEY=test-js-key bash scripts/verify.sh`를 실행해 종료 코드 0을 확인했다. 프런트 단위 420건, Java 458건, 개발 Chromium 50건, 배포 JAR Chromium 5건, 실제 MySQL 8건과 재시작 후 Chromium 7건이 통과했다. 실패·오류·건너뜀은 0건이다. 로그는 `/tmp/plugpass-t50-verify.log`다. 사용자 요청에 따라 Chromium은 headless로 실행했고 러너와 결과는 cmux pane에 표시했다.

CI·병합 결과는 PR에서 확인한다.
