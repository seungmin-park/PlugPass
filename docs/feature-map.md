# 기능과 검증 범위

| 경로 | 기대 결과 | 검증 |
| --- | --- | --- |
| GET /actuator/health | 200, status UP, 세부 정보 미노출 | 실제 HTTP 통합 테스트 |
| GET /actuator/env | 404 | 실제 HTTP 통합 테스트 |
| JPA 저장 → flush → context clear → 조회 | DB에서 동일한 내용을 새 객체로 조회 | H2 + 실제 Repository 테스트 |
| API 문서 생성 | 응답의 필드 설명이 일치해야 생성 성공 | HealthDocumentationTests + REST Docs |
| 패키징 JAR의 GET /docs/index.html | 생성된 HTML과 같은 문서, 200 | 실행 JAR HTTP 확인 |

JPA 엔티티와 Repository는 현재 테스트 전용이다. 충전소 업무 데이터·API는 아직 구현하지 않았다.
문서 테스트는 MockMvc 기반이며 실제 네트워크 테스트와 구분한다. 실제 HTTP 검증은 별도 테스트와 JAR 실행으로 수행한다.

문서에 status 설명을 누락시키면 REST Docs 테스트가 실패하는 것을 확인했다.
메모리 H2는 실행 중 데이터만 유지하며 재시작 후 보존은 검증 대상이 아니다.

실행 절차: [verify-plugpass](../.agents/skills/verify-plugpass/SKILL.md)
이번 실행 기록: [JPA·REST Docs 검증](jpa-restdocs-verification.md)
