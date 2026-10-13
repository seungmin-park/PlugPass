# T44c 이전 지도 구현 제거

## 결정과 데이터 흐름

운영 지도 PR70 배포 후 headless Chromium에서 카카오 SDK 로드, 지도 이미지 8개, 지도 오류·페이지 오류 0건을 확인했다. PR71 배포 후 같은 운영 주소에서 `서울시청` 후보를 선택해 검색 좌표가 해당 위치로 바뀌고 지도가 열리는 것도 확인했다. 이 검증을 마쳤으므로 이전 Leaflet/OpenStreetMap 후퇴 경로는 더 이상 유지하지 않는다.

```text
StationMap.vue → loadStationMap → Kakao SDK → 지도·마커·영역
                         ↑
                 KAKAO_JS_KEY → Vite 빌드·개발 검증
```

`StationMapSession`은 마커 갱신·중심 이동·영역·선택·해제 계약을 유지한다. 선택 View와 충전소 API는 구체 지도 SDK를 알지 않는다. 키가 없으면 과거 지도를 몰래 띄우지 않고 지도 오류를 안내하며 목록은 계속 쓸 수 있다. 공용 verify는 JavaScript 키가 없으면 즉시 실패해 지도 검증이 빠진 상태를 성공으로 보고하지 않는다.

## Red → Green → Refactor

- Red: 키 없는 환경에서 `loadStationMap()`이 카카오 키 오류를 내야 한다는 테스트는 실제로 `createLeafletStationMap`을 반환해 실패했다.
- Green: 선택 경계를 카카오 전용으로 바꾸고 동일 테스트 6개를 통과시켰다.
- Refactor: 지도 수명·영역·마커·키보드·실패 테스트를 카카오 SDK 모형으로 옮기고 Leaflet 코드·패키지·CSS를 제거했다. 실제 운영 지도와 E2E 모형의 범위를 구분한다.
- 키 누락 검증: `env -u KAKAO_JS_KEY bash scripts/verify.sh`는 지도 검증을 건너뛰지 않고 즉시 종료 코드 1을 반환했다.

전체 `CI=1 bash scripts/verify.sh`를 JavaScript 키가 제공된 환경에서 실행해 종료 코드 0을 확인했다. Java 458개, 프런트 410개, 개발 Chromium 39개, 패키지 Chromium 5개, MySQL 8개와 재시작 JAR Chromium 7개가 통과했다. `rg -i 'leaflet|openstreetmap' frontend scripts` 결과는 0건이다. CI·병합 결과는 PR에서 별도로 확인한다. 과거 T25/T30 기록과 원본 실패 로그는 당시 구현의 증거로 보존한다.
