# 기능과 검증 범위

| 경로 | 기대 결과 | 검증 |
| --- | --- | --- |
| GET /actuator/health | 200, status UP, 세부 정보 미노출 | 실제 HTTP 통합 테스트 |
| GET /actuator/env | 404 | 실제 HTTP 통합 테스트 |
| JPA 저장 → flush → context clear → 조회 | DB에서 동일한 내용을 새 객체로 조회 | H2 + 실제 Repository 테스트 |
| API 문서 생성 | 응답의 필드 설명이 일치해야 생성 성공 | HealthDocumentationTests + REST Docs |
| 패키징 JAR의 GET /docs/index.html | 생성된 HTML과 같은 문서, 200 | 실행 JAR HTTP 확인 |

JPA 엔티티와 Repository는 현재 테스트 전용이다. 충전소 업무 저장·API는 아직 구현하지 않았다. T02의 생성 시 검증하는 도메인 모델은 아래 테스트로 확인한다.
문서 테스트는 MockMvc 기반이며 실제 네트워크 테스트와 구분한다. 실제 HTTP 검증은 별도 테스트와 JAR 실행으로 수행한다.

문서에 status 설명을 누락시키면 REST Docs 테스트가 실패하는 것을 확인했다.
메모리 H2는 실행 중 데이터만 유지하며 재시작 후 보존은 검증 대상이 아니다.

개발 전달 경로: [작은 기능별 PR·검증·자동 머지](delivery-workflow.md). 공용 명령 `bash scripts/verify.sh`는 테스트 결과·패키징·실제 HTTP를 검사한다. GitHub 보호 규칙과 원격 CI의 실제 적용 여부는 로컬 성공과 별도로 확인한다.

실행 절차: [verify-plugpass](../.agents/skills/verify-plugpass/SKILL.md)
이번 실행 기록: [JPA·REST Docs 검증](jpa-restdocs-verification.md)

| T02 도메인 경로 | 기대 결과 | 검증 |
| --- | --- | --- |
| ChargerId 생성·Set 등록 | 세 식별자의 차이는 구분, 동일 값 중복 판정, 선행 0 보존 | StationModelTests, Spring/DB 없는 단위 테스트 |
| GeoPoint 생성 | 범위 양끝 포함, 바로 밖·NaN·무한대 거부 | StationModelTests |
| Station/Charger 생성 | 빈 식별자·이름, null 위치·ID 거부 | StationModelTests |

이 검증은 공공 API 인증 성공·충전소 JPA 저장·업무 HTTP 흐름을 증명하지 않는다.

| T03 경로 | 기대 결과 | 검증 |
| --- | --- | --- |
| 외부 상태 코드 → ChargerStatus | 공식 코드 매핑, 미지원/null은 UNKNOWN | ProviderStatusMapperTests 19건 |
| Charger 상태·원본 보존 | 정규화 값과 원본 분리, null 업무 상태 거부 | ProviderStatusMapperTests |
