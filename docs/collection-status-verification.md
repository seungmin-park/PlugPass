# T36 수집 준비·갱신 지연 안내

## 사용자 경로와 책임

검색 응답에 `collection`을 추가한다. Controller는 조회 결과와 수집 상태를 HTTP 응답으로 조립하고 CollectionStatusService는 실행 이력을 조회한다. CollectionHealth/CollectionSchedule이 최초 준비·24시간 전체 정보·600초 성공 공백·최근 실패를 판단한다. 프런트는 API 응답을 검증하고 상태 문구와 마지막 상태 수집 성공 시각을 표시한다. 별도 자동 요청을 만들지 않고 기존 검색 요청의 응답 수명을 따르므로 이전 응답 역전 방지와 재시도를 유지한다.

```text
검색 클릭 → 기존 검색 API → StationQueryService + CollectionStatusService
  → collection: NOT_READY / CURRENT / DELAYED
  → 목록 유지 + 준비/지연 안내 + 상태 수집 성공 시각
```

CURRENT는 수집 작업 정상이며 충전기 현장 상태 최신성 보장이 아니다. 충전기별 UNVERIFIED와 관측/수집 시각을 유지한다. 미실행은 수집 중이라고 표시하지 않는다. API에는 마지막 전체/상태 성공 및 최근 시도 시각·결과·300초 주기를 담는다. 예전 응답의 collection 누락/null은 호환하고, 값이 있으면 모든 필드 구조를 검증한다.

## Red → Green과 실패 검증

- 실제 DB 기반 CollectionStatusTests: 최근 성공/실패/600초 공백에서도 NOT_READY를 반환하는3개 assertion 실패 후 상태 계산을 구현했다.
- StationSearchHttpTests: `$.collection.state` 누락 실패 후 응답 조립과 REST Docs를 추가했다.
- 프런트: 정상/지연/미준비 문구3개 누락 및 깨진 collection 구조 허용의4개 실패를 확인한 뒤 구현했다.
- 375px 브라우저에서 지연 안내와 기존 목록을 유지하고 새로고침으로 정상 안내를 회복한다. API 응답 fixture로 수집 장애를 제어하며 실제 공공API 장애를 유발하지 않는다.
- 배포 JAR의 정확한 응답 비교는 새 필드까지 검사하도록 갱신했다. MySQL 재시작 검사에서 고정 fixture 시각과 실제 JAR 시각에 따른 CURRENT→DELAYED를 각각 검증하고, 시각에 따라 계산되는 state를 제외한 모든 저장값은 동일해야 한다.

## 검증 결과

`/private/tmp/plugpass-collection-ui`의 현재 cmux 보조 pane에서 `bash scripts/verify.sh`: 종료0. Java451·프런트354·Python32·개발 브라우저31·배포 JAR4·MySQL8·재시작 JAR7건, 실패/오류/skip0. 로그 `/tmp/plugpass-production/collection-ui-verify.log`와 build/verification/runtime.json, mysql/result.json. PR59 상태 수집에 의존하며, UI 기능만의 별도 PR로 전달한다. 원격 CI/실제 머지는 후속 기록한다.

이름·책임 검토: 조회 Controller에 시간 규칙을 넣지 않았고 상태 서비스가 기존 검색 Service의 쿼리 계약을 바꾸지 않는다. DTO는 응답 값, View는 표현과 기존 사용자 이벤트를 맡는다. 새 필드는 additive이며 상세·추천 응답은 바꾸지 않는다.

최종 책임 분리 후 동일 공용 verify를 다시 실행해 종료0을 확인했다. Service는 `dto.CollectionStatus`를 반환하고 HTTP 계층에서 CollectionStatusResponse로 변환한다. 최종 로그 `/tmp/plugpass-production/collection-ui-final-verify.log`, 검증 JAR sha256258d6c7e34f6349f482566dfb783866a66f883ea80a2dd422100644869db3ded. 테스트 건수·실패0은 동일하다.
