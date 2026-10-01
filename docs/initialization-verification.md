# 초기화 검증 기록 · 2026-10-01

- 환경: macOS arm64, Zulu Java 25.0.4.1 LTS, Gradle 9.7.1, Spring Boot 4.1.1.
- 최초 `./gradlew clean build --console=plain`: 종료 0.
- 노출 설정을 `health,env`로 임시 변경한 `./gradlew test`: 종료 1. 환경 경로 테스트에서 expected 404 / actual 200을 확인.
- 설정 복원 후 `./gradlew build --console=plain`: 종료 0. HTTP 통합 테스트 2개, 실패 0, 오류 0.
- 생성한 JAR를 Java 25, localhost 임의 포트로 실행. GET /actuator/health: HTTP 200, status UP. GET /actuator/env: HTTP 404.
- 실제 health 응답에는 groups: [liveness, readiness]도 포함된다. 최초 임시 검증 스크립트의 전체 JSON 동일성 검사는 이 추가 필드 때문에 실패했다. 상태와 상세정보 미노출 계약으로 검사를 수정한 뒤 통과했다. 애플리케이션 응답을 바꾸지는 않았다.
- 직접 시작한 검증 서버는 SIGTERM으로 종료했고 graceful shutdown 로그를 확인했다.
- cmux 식별 환경변수가 없고 `cmux identify --json`도 live socket을 찾지 못했다. 화면 E2E 대신 터미널에서 HTTP 검증을 수행했다.
- 테스트 의존성 Mockito의 동적 agent 연결 경고가 남아 있다. 현재 JDK 25 테스트는 통과하지만 향후 JDK 업그레이드 시 agent 설정을 검토한다.

## 증거

- `build/verification/build.log`
- `build/verification/guardrail-failure.log`
- `build/verification/guardrail-result.txt`
- `build/verification/versions.log`
- `build/verification/server.log`
- `build/verification/http-result.json`
- `build/test-results/test/TEST-com.plugpass.PlugPassApplicationTests.xml`

build 하위 파일은 로컬 검증 산출물이며 clean 실행 시 지워진다.
충전소 데이터 API·DB·추천·장애 복구 검증은 아직 수행하지 않았다.
