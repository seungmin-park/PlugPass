# T45 검색 선행 조건 안내

주소 후보를 선택하면 지도는 열리지만 차량 조건이 없을 때 조회는 시작되지 않았다. 기존 안내는 지도와 입력 양식 아래에 있어 주소 검색을 막 마친 사용자가 보기 어려웠다.

```text
주소·장소 검색 → 위치 후보 선택
                   ├─ 차량 조건 있음 → 기존 충전소 조회 → 결과
                   └─ 차량 조건 없음 → 검색창 아래 안내 → 차량 선택으로 이동
                                                       → 검색 제출 → 결과
```

`StationSearchView`가 위치 선택과 검색 시작 여부를 조정한다. `SearchForm`은 차량·어댑터·직접 확인 조건을 계산해 View에 전달한다. 안내는 검색창 다음, 지도 앞에 놓고, 위치 이름과 검색에 필요한 다음 행동을 함께 보여 준다. 버튼은 이미 열린 검색 조건의 차량 선택란으로 초점을 옮긴다. 지도 영역 재검색에서도 같은 안내를 사용한다. 조건이 완성되면 안내는 사라진다. 주소를 직접 고른 뒤 양식에서 검색하면 지도 준비 여부와 관계없이 그 주소 좌표를 사용하고, 명시적인 ‘이 지역 재검색’만 지도 영역을 사용한다. 기존 검색 API와 호환성 규칙은 그대로 사용한다.

현재 위치 버튼은 차량 선택란과 같은 양식 안에 있으므로 상단 안내를 중복 표시하지 않는다. 차량이 빠진 채 양식을 제출하면 기존 필드 오류가 바로 차량 선택란을 가리킨다.

## 검증 기록

- Red: `npm --prefix frontend run test:unit -- src/features/search/views/PlaceSearchView.spec.ts`에서 차량 없는 주소 선택 뒤 `.search-prerequisite-notice`가 없어 assertion 실패했다.
- 지도 경계 Red: `scripts/frontend-e2e.sh e2e/map-place-search.spec.ts --grep "차량 없이 지도"`에서 지도 재검색 뒤 안내 요소가 없어 assertion 실패했다.
- 주소 검색 Red: 차량 선택 후 검색하는 테스트에서 URL이 비어 있어 assertion 실패했다. 열린 지도의 영역을 우선하여 아직 준비되지 않은 지도 때문에 주소 검색을 막는 경로였다.
- Green: 같은 단위 테스트 4건 통과. 주소 선택 뒤 위치·안내·초점·DOM 순서·차량 선택으로 이동, 검색 API 미호출과 차량 선택 뒤 주소 좌표 조회를 확인했다.
- 모바일 E2E: 현재 cmux workspace `workspace:1000050003`의 보조 pane `surface:1000050024`에서 Chromium headless로 실행했다. 320px·375px에서 주소 선택→안내→차량 선택→실제 충전소 결과, 지도 재검색의 조건 부족 안내와 기존 현재 위치·차량 프로필 경로를 확인했다. 헤드리스 러너의 명령과 로그는 같은 pane에 표시했으며 브라우저 화면을 보이는 E2E로 주장하지 않는다.
- 최종 `bash scripts/verify.sh` 종료0: 프런트 단위411건, Java/H2 458건, 개발 모드 브라우저42건, 배포 JAR 브라우저5건, MySQL 저장 계약8건과 재시작 JAR 브라우저7건. 해당 결과의 실패·오류·건너뜀은 모두0이며, 실제 MySQL 재시작 뒤 데이터 보존을 확인했다. 로그: `/tmp/plugpass-t45-pass-verify.log`, `build/verification/`, `frontend/test-results/`, `build/test-results/`.

처음 대상 E2E 실행에서는 `VITE_KAKAO_JS_KEY` 전달을 빠뜨려 지도 SDK가 로딩되지 않았다. 테스트 환경 설정 오류로 분류하고 키 값을 출력하지 않은 채 주입을 고쳐 재실행했다. 이 실패를 제품 회귀로 판단하지 않았다.

전체 검증 첫 완주에서 현재 위치 선택 직후의 새 안내와 기존 양식 오류가 동시에 경고 역할을 가져 차량 프로필 E2E가 실패했다. 현재 위치 버튼은 양식 안의 차량 선택란과 가까워 새 상단 안내를 띄우지 않도록 조정했다. 같은 전체 검증을 다시 실행해 42개 개발 모드 브라우저 테스트를 모두 통과했다.

이름·책임 검토: `promptForVehicle`은 차량 조건이 빠진 주소·지도 검색 요청의 안내 상태·초점을 소유하고, `focusVehicleChoice`는 CTA의 이동만 맡는다. `needsVehicleSelection`은 주소 후보 또는 지도 영역에 필요한 차량 조건이 빠졌음을 나타낸다. 양식 제출은 선택 위치를, 지도 재검색 버튼은 지도 영역을 사용하므로 두 행동의 책임이 분명하다. URL·JSON·DB 공개 계약은 바뀌지 않는다. 지도 SDK 실패나 주소 검색 실패는 기존 경계의 오류 안내와 재시도에 맡긴다. iPhone Safari 실기기 동작은 이 Chromium 검증만으로 주장하지 않는다.

## PR #73 전달 및 운영 확인

[PR #73](https://github.com/seungmin-park/PlugPass/pull/73)의 `PlugPass verify`가 검증한 head `17d68db`에서 성공했고 native auto-merge로 main `b3197c7`에 병합됐다. 같은 main의 CI와 Production CD도 성공했다. 운영 URL의 375px Chromium headless에서 실제 서울특별시청 주소 후보를 선택하자 안내가 화면 y≈336px에 표시되고 초점이 안내→차량 선택으로 이동했다. 선택 전 URL에는 검색 조건이 없었고, 차량 선택 후 주소 좌표로 검색 API HTTP200·충전소20건·화면 카드20개를 확인했다. 페이지 실행 오류는0건이었다. 이 결과는 실제 iPhone Safari의 위치 권한이나 화면 동작을 증명하지 않는다.

## 선택한 주소에 맞는 지도 문구

운영 스크린샷에서는 주소를 이미 골랐는데도 “위치를 선택하지 않았다면…”과 아직 없는 카드·마커의 조작법이 표시됐다. 별도 `fix/search-map-step-guidance` 변경에서 검색 전 지도 기본 위치·지역 재검색 설명은 선택 주소가 없을 때만 보이고, 마커·카드 조작법은 충전소가 있을 때만 보이도록 한다. 주소 선택 전 지도만 연 경우에는 지역 선택 안내를 유지한다.

- Red: `PlaceSearchView.spec.ts`에서 주소 선택 뒤 기본 위치 설명이 남아 assertion 실패했다.
- Green: `PlaceSearchView.spec.ts`와 `StationMap.spec.ts` 대상10건 통과. 지도만 연 경우와 주소 선택 후의 문구 차이를 확인했다.
- 전체 `bash scripts/verify.sh` 종료0: 프런트 단위411·Java/H2 458·개발 모드 Chromium42·배포 JAR Chromium5·MySQL8·재시작 JAR Chromium7건. 실패·오류·건너뜀0. 로그는 `/tmp/plugpass-t45-map-copy-verify.log`와 `build/verification/`, `frontend/test-results/`, `build/test-results/`에 있다. 별도 PR·CI·병합 상태는 후속 PR에서 확인한다.
