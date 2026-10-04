# 4주차 검증 기록

2026-10-05. 승인 범위: tasks.md T14/T15. 기존 Java25·Boot4.1.1·JPA/H2·REST Docs 유지, 프런트엔드/TypeScript 적용 대상 없음. [실행 계획](week4-plan.md).

## T14

- 기반: b45ac0d, codex/week4-journey. `./gradlew test --tests '*ChargingJourneyTests' --console=plain`:3건. 실제 loopback 공급자 HTTP·XML·production 수집 commit·H2·사용자 HTTP. 상세 시나리오: [데모](demo.md).
- 신규 검사기 Red: `python3 -m unittest discover -s scripts -p 'test_verify_runtime.py' -v`. API가 없는 최초 실행은 AttributeError2건으로 실행 준비 오류였다. 빈 검사 함수로 callable 계약을 마련한 뒤 누락/빈 산출물에 RuntimeError가 발생하지 않는 assertion 실패2건을 실제 확인했다. [Red 출력](evidence/week4-t14-red.txt).
- Green: 필수7종 HTTP 증거 파일·JSON 목록 필드·정확한 건수를 검사한다. 기존 업무 로직은 변경하지 않았다. 종합 테스트의 STALE 코드 기대값 오류(SOURCE_OBSERVATION_TOO_OLD)를 기존 계약 MAX_AGE_EXCEEDED로 바로잡았다. 이 실패는 기능 Red가 아니다.
- Refactor/리뷰: 실제 HTTP 스레드가 읽는 테스트 시계·응답·관측 시각은 volatile로 전달한다. Service는 실제 production transaction, cleanup은 해당 클래스 소유의 자식→부모 삭제. Controller·HTTP DTO·저장 schema·라우트·직렬화 계약 변화 없음. 도메인 판단을 테스트 fake로 대체하지 않는다. 규모만 맞추는 객체 분리는 불필요했다.
- 성공/실패/경계: 중복·거리0/반경 밖·관측 시각 누락·601초 경과·503 후 저장/성공 시각 유지·다음 성공 회복. 인증/429/timeout/부분 실패·동시 실행은 기존 T05/T11/T12 필수 suite를 유지한다. 실공공 인증·현장 충전·재시작 보존은 별도 범위.
- 최종 `bash scripts/verify.sh`: Java356건·Python11건, 실패/오류/skip0. 실제 JAR health200·검색200·추천200·validation400·없는 상세404·기본 metrics404·env404·REST Docs 동일. 로그: `/tmp/plugpass-t14-verify-final.log`, `build/verification/server.log`, `runtime.json`, `charging-journey/*.json`, `build/test-results/test/`.
- cmux: 호출 env 없음·identify/ping/capabilities live socket 없음·escalated identify 동일. 표시 불가의 원인을 확인했고 터미널 HTTP로 검증했다. 화면 E2E와 실제 공급자 키 인증은 미확인이다.

[T14 PR#28](https://github.com/seungmin-park/PlugPass/pull/28) 필수 CI SUCCESS·2026-10-05 03:44 KST 실제 merge ed855d45. 검증 head2aec903의 evidence.py run/check 모두0·current. 원격 main의 검증 지침 통합 후 재검증했다. T15는 이 main에서 진행한다.

## T15 준비와 검증

- Summary Red: `python3 -m unittest discover -s scripts -p 'test_performance.py' -v` assertion3건 실패(0인 percentile, 빈 분포 허용, 요청 수0). [출력](evidence/week4-t15-summary-red.txt). 최소 구현 후3건 통과. warmup을 p95에 섞지 않고 모든 unexpected error를 포함한다.
- Dataset Red: `./gradlew test --tests '*PerformanceDatasetTests'` expected16/actual0 assertion1건 실패. 최초 IN_USE enum 이름 컴파일 오류는 OCCUPIED로 바로잡은 준비 오류이며 Red로 세지 않았다. 생성 저장을 구현해1건 통과했다.
- Refactor: 최초 데이터 준비의 JDBC insert를 도메인 Builder/실제 Repository saveAll로 교체해 생성 불변식과 현재 매핑을 따르게 했다. 로컬 수동 seed SQL 파일에 의존하지 않는다. 동일 테스트 통과와 전체 규모 smoke의 HTTP 정확성을 확인했다. Metadata는 test 전용 JDBC count·Statistics 읽기만 맡고 업무 Controller/Service는 유지한다.
- 이름·경계: PerformanceDataset은 생성/저장, PerformanceApplication은 test 서버 진입, load.py는 고정 요청/정확성/집계/프로세스 정리를 맡는다. SQL/HTTP/JSON/도메인 공개 계약과 production 코드 변경 없음. 테스트 전용 endpoint/package는 운영 JAR에서 제외한다. TypeScript/Vue 없음.
- 2users/0초 warmup/1초 smoke는 도구 동작 확인이며 기준선 결과가 아니다. 최종 전체/실측 결과는 아래에 기록한다.
