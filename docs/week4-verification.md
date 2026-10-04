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

PR/원격 CI/main 반영은 전달 후 기록한다. T15 측정은 T14 main 반영 다음에 진행한다.
