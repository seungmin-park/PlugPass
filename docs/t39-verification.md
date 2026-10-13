# T39 결과 우선 검색과 주소·지도 연결

2026-10-13. feat/search-results-first, /tmp/plugpass-feat-search-results-first. 기반 main68cf84c(T38 PR63 필수 CI 성공·실제 머지).

## 흐름과 책임

```text
주소 입력 → 후보 선택 → 선택 좌표/차량 조건 확정 → 주변 API 검색
                       ├─ 지도 표시·중심 이동
                       └─ 조건 접기·결과 제목 포커스 → 목록
지도 이동 → 이 지역 재검색 → 영역 검색
```

URL/store는 확정 검색을, View는 사용자 선택과 포커스를, 지도 세션은 표시/이동을 소유한다. 주소 후보 선택은 기존 영역 bounds를 새 주소의 반경 검색에 섞지 않는다. 응답 순서 방어는 기존 store를 유지한다. DOM 순서와 화면 순서를 달리 만드는 CSS 재배치는 사용하지 않는다.

## Red→Green과 리뷰

- `npm --prefix frontend run test:unit -- src/features/search/views/PlaceSearchView.spec.ts src/features/search/views/StationSearchView.spec.ts`: 주소 선택 후 요청1회(기대2회), 접힌 조건 패널 없음의 assertion2실패. /tmp/plugpass-t39-red.log.
- Green: 주소 선택 즉시 반경 검색/지도 열기, 선택한 장소명 유지, 검색 후 조건을 native details로 접기, 수집 세부는 목록 뒤 정보 갱신 안내로 옮기기. 지연/준비 전 안내는 계속 기본 노출한다.
- 첫 전체 실행은 리뷰에서 발견한 포커스 문제를 보완하기 위해 중단했다. 완료 결과로 사용하지 않는다.
- 독립 리뷰의 조건 접기 후 숨은 버튼 포커스 문제를 새 테스트로 재현했다. /tmp/plugpass-t39-focus-red.log: activeElement ID가 results-heading이 아닌 assertion 실패1건.
- Green: 사용자 검색에서 조건을 접고 nextTick 후 결과 제목으로 포커스 이동. API 오류 포커스를 덮지 않도록 error를 확인한다. 동일 조건 재검색도 같은 정책을 따른다.
- 영역 검색 중 NACS 선택→새 주소 선택→기존 bounds 제거·좌표/규격 유지·지도 실패 중 목록 유지 테스트 통과. 모바일320/375 직접 링크 및 주소 선택/지도 열린 경로를 각각 검증한다.
- 이름/책임 검토: selectPlace는 주소 선택 유스케이스 조정, confirmSearch는 URL 확정/재요청/포커스, 지도/저장/호환 정책을 소유하지 않는다. 공개 HTTP/URL 계약 변경 없음. 요청 순서·에러·빈 결과 검증은 기존 suite 유지.

## 실행 결과

프런트358건 통과. 새 주소 선택 모바일 E2E는 첫 카드 y=708.33px로 기준650px 미달을 재현했다. 검색 후 지도 중복 안내 생략과 모바일 지도180px로 개선하고 동일 기준으로 재검증 중이다. 최종 공용 verify·cmux·PR/CI/머지는 실행 뒤 추가한다. 필수 테스트 파일은 기존 파일 안에 추가해 누락 검사를 유지했다.

## 화면과 외부 경계

기존 보조 pane이 닫혀 재확인 후 같은 호출 workspace1000050003에 pane1000050011/surface1000050020을 생성했다. 전체 verify는 해당 터미널에서 headed로 실행한다. 주소 후보 성공은 API 경계 fixture이고 실제 카카오 인증/주소 검색 품질 검증은 아니다. 실제 지도 이동/충전소 API·DB 검증과 구분한다. iOS 실제 권한·외부 지도 앱 전환은 T38/T40의 실기기 확인으로 남긴다.

추가 검증 과정: 주소 선택 모바일2건은 동일650px 기준으로 통과했다. 기존 키보드 suite의 새 조건 summary 탭 정류장 누락을 수정해 실제 순서 assertion을 보강했다. cmux 브라우저surface1000050021은 document.visibilityState=hidden이므로 화면을 보였다고 표현하지 않는다. 별도 headed 러너/로그는 보조 터미널에서 실행했다.

배포 JAR 검증은 조건이 항상 펼쳐진 것으로 가정한 기존 assertion1건이 실패했다. 새 계약대로 접힘을 assertion한 뒤 조건 변경 클릭→주소 입력 노출을 검증하도록 수정했다. 배포 검증을 생략하거나 개발 검증으로 대체하지 않았다.

## 최종 결과

공용 `bash scripts/verify.sh` 종료0. Java451·프런트358·개발브라우저36·배포JAR4·MySQL8·재시작JAR브라우저7, 실패/오류/skip0. 타입/lint/build 통과. /tmp/plugpass-t39-verify.log·build/verification·build/test-results·frontend/test-results에 원본 결과가 있다. 수정 후 독립 재리뷰에서 남은 차단 결함 없음. [PR64](https://github.com/seungmin-park/PlugPass/pull/64) 필수 CI 성공 뒤 main9a0126f에 실제 병합했다.
