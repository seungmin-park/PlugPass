# T30/T31 지도 영역·주소 검색 검증

2026-10-13. T28/T29 `533616e` 위에서 구현했다. 사용자는 지도 재검색과 주소 검색 모두, 카카오 연동 후 키는 나중에 설정하는 방향을 선택했다. 개인 어댑터 조사는 [T32 별도 기록](adapter-ownership-research.md)으로 남겼으며 호환 검색 구현 범위를 확대하지 않았다.

## 바뀐 사용자 흐름

```text
지도 보기 → 이동/확대 → 이동 완료 → 이 지역 재검색 → 지도 안 충전소
주소·장소 입력 → 검색 → 위치 후보 선택 → 주변 충전소 검색
                         └ 지도 표시 중이면 선택 위치로 지도 이동 → 영역 검색
```

반경 선택 UI를 제거하고 기존 반경 API/직접 URL·추천 흐름은 유지한다. 최초 지도는 접혀 있으며 검색 전에도 열 수 있다. 위치 미선택 시 서울 시청을 시작점으로 쓰고 실제 현재 위치라고 표현하지 않는다. 예시 좌표는 합성 데모 지역임을 유지한다. 너무 넓은 영역은 조용히 잘라내지 않고 확대를 안내한다. 지도 이동만으로 결과를 갱신하지 않는다.

## 실제 Red → Green

| 동작 | 실제 실패와 최소 구현 |
| --- | --- |
| 영역 HTTP | `./gradlew test --tests "com.plugpass.search.integration.StationAreaFlowTests"`: 기존 `/stations/area`가 ID 바인딩 경로로 처리되어200 기대에400,3건 실패. 경계 DTO·도메인·조회 Service·Controller를 추가해 통과. |
| 영역 UI/API | 새 MapAreaSearch·stationAreaApi7건: 반경 UI 잔존, 지도 영역 이벤트 부재, 기존 API 경로 등의 assertion 실패. 반경 선택 제거, 실제 bounds 이벤트, 영역 API/URL을 연결. |
| 검색 전 지도 | MapSearchView2건: 초기 지도 열기 버튼 부재 assertion 실패. 결과 여부와 독립된 지도 탐색을 제공. |
| 카카오 키 없음 | PlaceSearchFlow1건: 새 경로가 없어503 기대에404. 키 미설정 안내를 추가하고 기존 충전소200을 함께 확인. |
| 외부 HTTP | KakaoPlaceSearchClient12건: 임시 client가 NOT_CONFIGURED만 반환하여 실패. 실제 JDK HTTP·JSON 계약·오류 분류를 구현해13건(키 없음 포함) 통과. |
| 주소 UI | PlaceSearchView2건: 주소 입력 부재 assertion 실패. 후보 검색·선택과 검색 확정을 분리해 연결. |
| 추천 계약 | 영역 정보가 기존 추천 API params에 섞이는 assertion1건 실패. API에서 중심·반경·커넥터·limit만 명시적으로 전달. |
| 이동 도중 재검색 | cmux 직접 이동에서 발견. MapSearchView의 실제 Leaflet 키보드 이동 직후 버튼 비활성화 기대가 실패. movestart에는 미확정 영역을 비우고 moveend에서 다시 제공. 일반 검색 제출도 이동 중에는 거부. 대상6건 통과. |

Red 원본에서 ANSI만 제거한 기록은 [evidence/t30](evidence/t30/)에 있다. 새 도메인/HTTP 경계·정렬 테스트는 구현 후 보강 검증이며 최초 Red라고 주장하지 않는다. 기존 SDK 테스트는 결과 변경마다 새 SDK를 로딩한다는 가정을 제거하고 단일 로딩 완료 후 최신 결과만 표시하는 공개 동작을 확인하도록 바꿨다. 반경 라디오 UI 테스트는 새 커넥터·지도 계약으로 바꾸되 기존 반경 URL/API 테스트는 유지했다.

Refactor: 지도 세션은 생성/마커 갱신/중심 이동/영역 복원/해제를 맡는다. 검색 결과를 이유로 지도 객체를 다시 만들지 않는다. StationQueryService의 공용 결과 조립은 SearchQuery 대신 중심 좌표를 받아 반경·영역 양쪽에서 재사용한다. 새 추천 테스트의 최초 assertion이 API 오류 변환에 감싸졌으므로 adapter 호출 뒤에서 계약을 검증하도록 바로잡고 명확한 assertion Red를 재실행했다.

## 최종 실행 결과

현재 cmux 검증 터미널에서 `bash scripts/verify.sh` 실행, `/tmp/plugpass-t30/verify-final-movement.log`, 종료0.

| 검사 | 결과 |
| --- | --- |
| Java 전체 |423건, 실패/오류/skip0|
| 프런트 전체 |350건/36파일, 실패/skip0|
| Python 검증 도구 |32건|
| 타입·lint·build |통과, lint 경고0, REST Docs·웹앱 JAR 포함|
| 개발 모드 headed 브라우저 |30건|
| 빈 DB 배포 JAR 브라우저 |4건; 영역 재검색/직접 링크 복원 포함|
| 실제 MySQL |7건,8.4.11, 재시작 뒤 데이터/준비 상태 보존|
| 재시작 MySQL 배포 JAR 브라우저 |7건; 기존 전체 사용자 흐름/길찾기/로딩|

[소스별 SHA·runtime·MySQL·JAR digest](evidence/t30/local-summary.json)를 최종 소스와 연결했다. 이 기록은 입력 소스 일치를 돕지만 요구사항 완전성 자체를 증명하지 않는다. 지도 이동 경계 수정 전의 공용verify도 종료0이었으나 프런트349건의 이전 결과이며 최종350건으로 대체했다.

브라우저 최초 실행30건 중1건은 visually hidden 라디오 입력을 직접 클릭해 타임아웃했다. 실제 사용자가 클릭하는 NACS 라벨로 수정했다. 강제 클릭·assertion 제거 없이 최종30건 통과를 확인했다.

## 현재 cmux 실제 화면

호출workspace1000050003·호출surface1000050003을 identify로 확인하고 기존 보조surface1000050012에서 러너·서버 명령/로그를 실행했다. 브라우저는 surface1000050013을 재사용했다. 브라우저가 보조 pane의 다른 탭에 있어 hidden 상태에서 지도 애니메이션이 일시 정지한 것을 확인했다. 실제 visible 상태에서 이동 완료/검색을 다시 확인했고 마지막에는 기존 브라우저를 같은 workspace의 아래 pane으로 분리해 로그와 화면을 함께 볼 수 있게 했다. 호출 터미널이나 다른 workspace에 테스트 명령을 보내지 않았다.

- 실제 주소 입력/검색으로 키 미설정 안내 확인, 예시 위치 선택 후 실제 외부 OSM 타일4장·마커2개 확인.
- 375px에서 가로 넘침 없음. 지도 확대·키보드 오른쪽 이동, 이동 중 재검색 비활성, 재검색 전 URL 유지, 완료 후 재검색으로 west가126.99302673339845→126.99645996093751로 변경됨을 확인.
- 예상 밖 console 경고/오류·브라우저 실행 오류 없음. [최종 지도 화면](evidence/t30/mobile-map-search.png).
- 자동 E2E는 재현성을 위해 타일과 주소 후보 성공 응답을 제어한다. 충전소 API·H2는 실제 실행하며 이 fixture를 실제 카카오 인증 결과나 실제 지도 배경으로 표현하지 않는다.

## 경계·책임 검토와 남은 범위

- 정상/빈 결과/키 없음/인증/429/5xx/timeout/깨진 JSON/잘못된 좌표/순서 역전·reset을 분리한다. 좌표 경계 포함, 필터 후 limit, 동일 거리 ID 정렬을 실제 DB·HTTP로 확인한다.
- Read-only 조회라 DB 쓰기 멱등성·부분 commit·상태 전이는 적용하지 않는다. 외부 자동 재시도는 없으며 타임아웃 후 사용자 재검색이다. 주소 Service는 DB를 사용하지 않아 트랜잭션을 열지 않는다.
- Controller는 request validation과 DTO 반환, Service는 조회 조정, MapSearchArea는 영역 불변식, Kakao client는 인증/HTTP/응답 변환을 소유한다. 프런트 Store가 비동기 상태·취소·응답 순서를 맡고 Leaflet 어댑터가 지도 수명을 맡는다. 추측성 차량/어댑터 인터페이스는 추가하지 않았다.
- 기존 JSON·DB·반경 URL 계약을 유지하고 새 `/stations/area`, `/places` 계약을 추가했다. 이름/책임/배치와 호출부를 검토했으며 REST Docs·전체 회귀로 공개 계약을 확인했다.
- 실카카오 키/계정 쿼터/실제 검색 품질은 미검증이다. 휴대폰 앱 실행은 T28 실기기 확인 항목으로 남는다. 차량/어댑터 호환 카탈로그는 T32 후속이다.
- T28/T29 PR[#55](https://github.com/seungmin-park/PlugPass/pull/55), head533616e의 필수 CI는 [SUCCESS](https://github.com/seungmin-park/PlugPass/actions/runs/38354807231/job/115155087164). 지도/주소 검색 PR·최종 원격 CI는 전달 시 별도로 기록한다. main CI→production 배포가 활성화되어 있어 머지는 배포 승인 뒤 진행한다.
