# T44b 카카오 주소·장소 검색과 지도 연결

## 사용자 흐름

```text
상단 주소·장소 입력 → 2자·300ms 뒤 카카오 후보 → 후보 선택
                                               ├─ 지도 중심 이동
                                               └─ 차량 조건이 있으면 충전소 검색 → 첫 결과
```

운영에서 서버 `/api/v1/places`는 `PLACE_SEARCH_NOT_CONFIGURED` 503으로 확인됐다. 지도에 이미 쓰는 JavaScript SDK의 `services` 라이브러리로 주소·키워드를 검색하면 브라우저에서 후보를 선택할 수 있다. REST 키는 브라우저에 넣지 않는다. 키 없는 로컬 환경과 기존 서버 API/REST Docs 계약은 유지한다. 서버 운영 REST 키 미설정은 이 변경 후에도 API 직접 호출에서 남으므로 별도 운영 설정으로 추적한다.

`PlaceSearch`는 입력·후보 표시와 debounce, `placeSearchStore`는 요청 순서·취소·상태, `kakaoPlaceSearch`는 SDK 결과 검증·중복 제거·오류를 맡는다. 선택 좌표는 `StationSearchView`의 단일 `selectedLocation`으로 들어가 지도 중심과 API 검색이 갈라지지 않는다. SDK 콜백은 취소할 수 없지만 AbortSignal과 요청 순번으로 늦은 응답이 화면을 덮지 않게 한다. 공급자 `ERROR`·8초 timeout·잘못된 좌표는 빈 결과로 숨기지 않는다.

## 검증 기준

- Red: SDK의 주소/장소·중복·오류·취소, SDK 키가 있을 때 서버 REST를 호출하지 않는 경계, 접힌 조건 밖 검색창, 입력 debounce가 각각 기존 동작에서 실패했다.
- Green: `libraries=services` 로딩, SDK 검색 변환, 검색창 재배치와 입력 후보를 연결했다.
- 예외: 빈 결과는 빈 후보 안내, SDK/공급자 실패는 재시도 가능한 오류로 표시한다. 키 없는 환경은 기존 명시적 서버 검색을 유지한다.
- 전체 검증: `CI=1 bash scripts/verify.sh`를 카카오 JavaScript 키가 제공된 환경에서 실행해 종료 코드 0을 확인했다. Java 458개, 프런트 410개, 개발 Chromium 39개, 패키지 Chromium 5개, MySQL 8개와 재시작 JAR 브라우저 7개가 통과했다. 개발 브라우저의 320·375px 주소 선택은 결과 카드가 화면에 나타나는지 확인했다.
- CI·운영 도메인의 실제 주소 후보는 PR 및 배포 결과와 별도로 기록한다.

공식 근거: [카카오 지도 Web SDK services](https://apis.map.kakao.com/web/guide/), [Places·Geocoder·Status](https://apis.map.kakao.com/web/documentation/).
