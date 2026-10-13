# T42a 여러 규격 검색·추천 API

2026-10-13. `feat/multi-connector-search`, 작업 트리 `/tmp/plugpass-feat-vehicle-compatibility`. T41 PR66가 병합된 `main`2129ed3에서 시작했다.

## 책임과 데이터 흐름

```text
단일 connector 또는 복수 connectors → 공통 HTTP 검증 → ConnectorSelection
                                              ├─ 공급자 코드 합집합 → JPA 충전기 조회
                                              └─ 추천 CandidatePolicy의 동일한 호환 판단
```

`ConnectorSelection`은 중복 없는 선택 규격과 공급자 코드 합집합을 소유한다. 요청 DTO는 단일/복수 중 하나만 입력받고, 공통 validator가 누락·충돌을 400 필드 오류로 바꾼다. Service는 기존 거리·ID 정렬과 limit 순서를 유지한다. Repository는 코드 합집합을 한 번 조회하므로 조합형 충전기를 규격별로 중복 합산하지 않는다. 차량 정보와 실제 어댑터 선택 UI는 T42b에서 이 API에 연결한다.

## Red → Green → Refactor

- `StationSearchHttpTests`에서 복수 입력은 400, 단일·복수 혼합은 200으로 나오는 실제 실패를 확인했다. 초기 혼합 테스트 fixture의 mock 반환 누락은 NPE라 기능 Red로 세지 않고 고친 뒤 두 assertion 실패를 다시 확인했다.
- Green: `ConnectorSelection`, 공통 요청 검증, 검색/영역/추천 전달과 추천 호환 판단을 추가했다. 기존 단일 URL 회귀를 유지했다.
- 실제 Service/Repository 테스트에서 공급자 코드06이 둘 이상의 선택 규격에 걸려도 충전기 한 대로 남고, 코드02와 합쳐진 뒤 limit이 적용됨을 확인했다. 검색·지도 영역·추천 MVC 요청과 쉼표 목록도 검증했다.
- `CandidateCharger`의 기존 단일 메서드는 다중 정책에 위임해 상태·제한·최신성 판단 중복을 없앴다. 공개 응답 DTO·정렬 계약은 변경하지 않았다.
- 좁힌 MVC 테스트 뒤 Asciidoctor만 실행하면 다른 문서 스니펫이 없어 실패했다. 전체 Java 테스트로 모든 스니펫을 만든 뒤 `./gradlew test asciidoctor`가 종료0으로 통과했다. 이 실패는 기능 Red나 전체 검증 결과로 사용하지 않는다.

정상: 단일/복수/쉼표 입력, 코드 합집합, 검색·추천 일치. 실패: 둘 다 없음·혼합·지원하지 않는 값. 경계: 조합 코드 중복, limit, 기존 단일 URL. 외부 API·시간 오류는 변경하지 않아 기존 회귀 범위다.

## 실행 결과

`CI=1 bash scripts/verify.sh`를 현재 cmux workspace `workspace:1000050003`의 보조 surface `surface:1000050020`에서 실행해 종료 0을 확인했다. `CI=1`은 Playwright의 Chromium을 headless로 실행한다. 화면 대신 실행 명령·assertion·로그를 cmux 터미널에 표시했으며, headless 브라우저가 눈에 보이는 화면에서 움직였다고 주장하지 않는다. 원본 로그는 `/tmp/plugpass-t42a-verify.log`, 종료 코드는 `/tmp/plugpass-t42a-verify.exit`이다.

- Java 458개, 프런트 단위 382개, 타입 검사·lint·build, REST Docs 통과.
- 개발 서버 Chromium 37개, 배포 JAR Chromium 4개 통과.
- MySQL 8개, MySQL 재시작 뒤 배포 JAR Chromium 7개 통과. 데이터·준비 상태 유지 확인.
- 필수 CI와 보호된 자동 머지 결과는 PR에서 별도로 확인한다.

이름·책임·공개 계약: `ConnectorSelection`이 중복 제거·코드 합집합을 담당하고, HTTP DTO가 단일/복수 입력 충돌을 검증한다. 기존 `connector` 요청과 응답 필드·정렬·limit은 유지한다. `connectors`를 지정한 새 요청만 합집합 조회로 동작한다. 외부 수집 실패·시계 처리에는 변화가 없어 해당 경계는 기존 공용 회귀 검증에 맡겼다.
