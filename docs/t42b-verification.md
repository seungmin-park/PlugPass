# T42b 차량 정보·이번에 사용할 어댑터

2026-10-13. `feat/vehicle-profile`, 작업 트리 `/tmp/plugpass-feat-vehicle-profile`. T42a [PR67](https://github.com/seungmin-park/PlugPass/pull/67)이 필수 CI 성공 후 병합된 main9dc615d에서 진행한다.

## 근거와 적용 범위

- [현대 2020년 3월 국내 코나 일렉트릭 카탈로그](https://www.hyundai.com/content/dam/hyundai/kr/ko/data/vehicles/catalog/kona-electric-catalog.pdf)는 충전구를 ‘콤보 방식’으로 표시한다. 따라서 이 모델·연식·시장의 직접 검색은 DC 콤보만 자동 지정한다. 완속 케이블 존재만으로 다른 충전기 규격을 자동 추가하지 않는다.
- [Tesla 대한민국 공공 충전 안내](https://www.tesla.com/ko_kr/support/charging/public-charging)는 CCS 콤보 1, CHAdeMO, J1772 어댑터가 별도이며 충전기·차량·정격 조건에 따라 실패할 수 있다고 안내한다. Tesla 차량의 기본 직접 규격이나 모든 연식의 어댑터 지원은 이 페이지로 확정할 수 없다. 그래서 직접 규격은 자동 지정하지 않고, 이번에 사용할 어댑터 선택과 차량 지원 확인이 모두 필요하다.
- [커뮤니티 사용 맥락 조사](adapter-ownership-research.md)는 같은 차량 계열에서도 어댑터 보유·휴대·실사용이 다름을 보여 준다. 통계적 보유율이나 제조사 호환 근거로 사용하지 않는다. 목록에 없는 차량은 차량 설명서를 보고 직접 규격을 선택하는 경로를 제공한다.

## 책임과 흐름

```text
내 차량 선택 → vehicleCatalog(공식 출처·시장·연식) → vehicleCompatibility(직접/어댑터/확인 필요)
                                               └→ SearchForm(이번 사용 선택) → StationSearchView(검색 확정·프로필 저장) → URL/검색 API
URL → criteria(검증·복원) → StationSearchView → 지도/카드·상세·추천 API
프로필 저장 실패 또는 오래된 ID → 저장값 무시, 현재 화면 입력·검색 유지
```

결과는 차량 단위로 직접 연결 또는 선택한 어댑터를 사용할 조건을 설명한다. 충전기별 실제 기기·정격 호환까지 확정하지 않는다. 현재 카탈로그가 좁기 때문에 목록에 없는 차량에는 기술 규격의 직접 확인이 여전히 필요하다.

## Red → Green → Refactor

- 정책 테스트에서 국내 코나의 직접 규격, Tesla의 어댑터 미선택/선택·차량 지원 확인, 미지원 조합·오래된 ID를 먼저 정의했다. 파일 부재 오류는 기능 Red로 세지 않고 최소 스텁 뒤 실제 assertion 3건 실패를 확인했다. 카탈로그와 정책으로 Green 4건 통과.
- URL 테스트는 복수 규격·차량 ID·어댑터 ID 왕복과 빈/중복/혼합 오류에서 실제 assertion 2건 실패 후 통과했다. 검색·추천 API 테스트는 두 엔드포인트가 단일 규격을 함께 보내는 실제 실패 2건을 확인한 뒤 수정했다.
- 폼 테스트는 첫 방문의 차량 선택 요구, 국내 차량 자동 선택, Tesla 어댑터 확인에서 실제 실패를 확인한 뒤 구현했다. 상태 watcher가 자신을 반복 갱신해 Vue 경고로 실패한 것을 모드 초기화와 조건 알림 책임으로 나눠 해결했다.
- 화면 테스트는 복수 규격 URL 복원과 결과·상세 링크 계약에서 실제 실패를 확인했다. 주소 선택의 빈 어댑터 목록이 URL에서 거부되는 회귀를 전체 프런트 테스트에서 발견해 파서와 경계 테스트를 수정했다.
- 검토에서 폼 제출만 프로필을 저장해 주소 선택과 지도 재검색이 빠지는 결함을 찾았다. 두 경로의 저장과 주소 선택 시 기존 프로필 삭제를 먼저 assertion하여 3건의 실제 실패를 확인했다. 저장 책임을 모든 경로가 공유하는 `confirmSearch`로 옮긴 뒤 대상 6건이 통과했다. 경로 이동 후 동일 화면에서 빈 검색으로 돌아와도 갱신된 저장값을 쓰도록 저장 상태를 반응형으로 두었다.
- 첫 공용 verify는 개발 Chromium 39건 중 35건 통과·4건 실패로 종료1이었다. 기존 모바일 길찾기 두 건은 새 첫 방문 화면에서 차량을 선택해야 하며, 새 테스트 두 건은 라우터 쉼표 인코딩·숨긴 라디오 클릭/스크롤 좌표의 기대가 실제 UI와 달랐다. 조건을 정확히 고친 뒤 cmux에서 해당 Chromium headless 5건이 모두 통과했다. 이후 전체 재검증 결과는 아래에 기록한다.

정상: 기존 단일 링크, 차량 직접 규격, 여러 어댑터, 주소/지도/검색/상세/추천 연결. 실패: 선택 전 제출, 어댑터 미선택·차량 지원 미확인, 오래된 ID·깨진 URL·저장소 차단. 경계: 빈 어댑터, 중복 규격, 320px·reduced motion, 응답 역전. 외부 충전기·실제 어댑터 작동과 실기기 설치 앱은 자동 검증 범위 밖이다.

## 전체 실행 결과

`CI=1 bash scripts/verify.sh` 최종 종료0. Java 458건, 프런트 단위 399건, 개발 Chromium 39건, 배포 JAR Chromium 4건, 실제 MySQL 8건과 재시작 JAR Chromium 7건이 모두 통과했다. 프런트 타입 검사·lint·build와 JAR 패키징도 통과했다. 실행 명령·로그는 현재 cmux workspace의 보조 터미널 `surface:1000050020`에 표시했고, 브라우저는 사용자 요청대로 headless로 실행했다. 최종 로그 `/tmp/plugpass-t42b-verify.log`, 종료 코드 `/tmp/plugpass-t42b-verify.exit`, 단위 결과 `frontend/test-results/unit.json`, 브라우저 보고서 `frontend/playwright-report`, Java XML `build/test-results/test`.

필수 CI·보호된 자동 병합 결과는 PR 실행 후 확인한다. 역할·이름 검토: `SearchForm`은 입력과 이벤트, `StationSearchView`는 검색 확정과 저장, `vehicleCompatibility`는 공식 근거의 조건 판단, API 모듈은 HTTP 전달을 맡는다. 기존 단일 `connector` URL/HTTP 계약은 유지하고 복수 규격만 `connectors`를 사용한다. 실제 iOS 위치 권한 변경, 외부 지도 앱 실행, 현장 어댑터 작동은 이 Chromium/서버 검증으로 증명되지 않는다.
