# T43 위치 선택 표시와 iOS 권한 조사

2026-10-13. `fix/location-feedback`, `/tmp/plugpass-fix-location-feedback`.

## 확인한 원인과 범위

실제 iPhone Safari에서 첫 시도에 ‘위치 권한이 거부되었습니다’가 표시됐고 이후에는 좌표가 반환됐다. 운영 주소는 HTTPS 200이고 위치를 금지하는 Permissions-Policy 응답 헤더가 없다. 배포된 자바스크립트는 클릭마다 `getCurrentPosition`을 호출하며 앱이 이전 거부를 저장하지 않는다. 브라우저의 오류 코드 1은 기기 위치 서비스·브라우저·사이트 권한을 하나로 묶어 반환할 수 있어, 어느 설정이 첫 거부를 일으켰는지는 기기 설정 없이는 확정할 수 없다. [W3C Geolocation](https://www.w3.org/TR/geolocation/)은 브라우저 허용 후에도 운영체제 권한 거부가 가능하다고 설명한다. [Apple 위치 서비스 안내](https://support.apple.com/en-us/102515)와 [Safari 사이트별 권한 안내](https://support.apple.com/en-euro/guide/iphone/iphb01fc3c85/ios)를 따른다.

실제 성공 뒤 화면이 `현재 위치: 37.43016974031662 / 126.795371066941`처럼 내부 좌표를 노출한 것은 표시 문제다. 검색/지도 중심에는 정확한 좌표를 그대로 전달하고, 화면에는 `현재 위치 선택됨` 또는 선택한 장소 이름만 표시한다. 별도 역지오코딩을 호출하거나 근거 없는 동네 이름을 추정하지 않는다.

```text
Safari 위치 응답 → StationSearchView의 selectedLocation(정확한 좌표) → 검색 URL/API·지도 중심
                                                        └→ 사용자 표시: 위치 이름 + 선택됨
```

## Red → Green → Refactor

`StationSearchView.spec.ts`와 `PlaceSearchView.spec.ts`의 위치 선택 문구를 먼저 기대값으로 바꾸고 실행했다. 두 assertion이 실제 좌표 노출 때문에 실패했고 나머지 20건은 통과했다. 템플릿의 표시 부분 한 곳을 바꾼 뒤 두 파일 22건이 통과했다. 새 객체나 별도 표시 책임은 필요하지 않아 구조는 유지한다. 검색 좌표의 URL/API 전달은 기존 화면·브라우저 테스트로 확인한다.

`CI=1 bash scripts/verify.sh` 종료0. Java 458건, 프런트 단위 399건, 개발 Chromium headless 39건, 배포 JAR 4건, 실제 MySQL 8건과 재시작 JAR 7건이 통과했다. 타입 검사·lint·build도 통과했다. 명령과 로그는 호출 workspace의 새 보조 터미널 `surface:1000050024`에서 보였고, 브라우저는 사용자 요청대로 headless였다. 로그 `/tmp/plugpass-location-verify.log`, 종료 코드 `/tmp/plugpass-location-verify.exit`, 브라우저 보고서 `frontend/playwright-report`, Java XML `build/test-results/test`.

Refactor에서는 `StationSearchView`가 이미 위치 선택 상태와 사용자 문구를 소유하므로 책임을 옮기지 않았다. 호출부의 정확한 좌표·URL·API 계약은 변경하지 않았고 기존 사용자 흐름 테스트로 확인했다. 위치 권한 거부의 정확한 iOS 설정 원인은 기기 설정 확인 전까지 미확정이다. PR/CI 결과는 생성 후 확인한다.
