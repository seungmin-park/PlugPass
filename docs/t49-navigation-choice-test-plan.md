# T49 검색 목록 길찾기 선택 검증 계획

## 사용자 흐름과 책임

```text
검색 결과 카드 길찾기 → 검색 화면의 충전소 선택 → 하단 선택 패널
  → 카카오맵 / 네이버 지도 / 웹 지도 → 외부 앱·웹으로 목적지 전달
```

카드는 길찾기 URL을 생성하지 않는다. 검색 화면은 선택한 충전소만 소유한다. 패널과 길찾기 컴포넌트는 목적지별 앱 링크와 웹 대안을 표시한다. 선호 앱 설정은 상세 화면의 바로가기에만 사용한다.

## 확인할 사례

| 상황 | 기대 결과 | 실행 근거 |
| --- | --- | --- |
| 모바일 첫 검색 결과에서 길찾기 | 같은 충전소의 패널과 앱 선택 링크가 표시된다 | `navigation.spec.ts`, `StationMapSelection.spec.ts` |
| 앱을 한 번 선택하고 목록에 복귀 | 다음 카드에서도 직접 이동하지 않고 선택 패널이 열린다 | `navigation.spec.ts`, `StationMapSelection.spec.ts` |
| 다른 마커 선택 | 패널 목적지와 링크 좌표가 함께 바뀐다 | `StationMapSelection.spec.ts` |
| 패널 닫기 | 원래 길찾기 버튼으로 초점이 복귀한다 | `StationMapSelection.spec.ts` |
| 데스크톱 | 앱 링크 대신 웹 지도 링크를 패널에서 선택한다 | `navigation.spec.ts` |
| 외부 앱 없음 | 웹 지도·설치 안내가 남는다 | `navigation.spec.ts` |

외부 앱의 실제 실행은 브라우저 자동화로 증명할 수 없으므로 목적지 URL 전달까지 검증한다. 이전 선택·비동기 검색 역전과 GPS 실패는 기존 검색 테스트가 검증한다. HTTP·DB 계약은 이 변경에 포함되지 않는다.

## TDD·실행 기록

Red: `npm run test:unit -- src/features/search/views/StationMapSelection.spec.ts`에서 앱 선택 후 카드가 버튼이 아니라 링크로 바뀌어 새 assertion이 실패했다. Green: 카드 길찾기를 버튼 이벤트로 통일하고 검색 화면에서 선호 기반 URL 생성을 제거했다. 대상 20건, 전체 프런트 단위 415건 통과. 이름·책임 검토: `directionsHref`는 검색 화면이 외부 이동 결정을 소유하게 하므로 제거했다. 카드의 `directions` 이벤트는 실제 수행 내용과 일치한다. 상세 화면·URL 계약은 유지한다.

공용 verify는 현재 cmux 작업 공간 `workspace:1000060002`의 보조 터미널 `surface:1000060010`에서 실행했다. 개발 Chromium 49건, 배포 JAR Chromium 5건, MySQL 재시작 Chromium 7건, 프런트 단위 415건, Java 458건, 실제 MySQL 8건이 통과했고 실패·오류·건너뜀은 0건이었다. 로그는 `/tmp/plugpass-t49-verify.log`다. Chromium은 사용자 요청에 따라 headless로 실행하고 러너 명령과 결과를 cmux pane에 표시했다. 실제 지도 앱 실행은 실기기 운영체제 경계라 자동화 범위에 포함하지 않는다.

CI·병합 결과는 PR에서 확인한다.
