# PlugPass · 플러그패스

전기차 충전소의 상태와 정보 최신성을 확인해 헛걸음을 줄이는 백엔드 프로젝트.

## 기술 스택

- Java 25 LTS
- Spring Boot 4.1.1
- JPA · H2
- Spring REST Docs

## 실행

JDK 25 환경에서:

```bash
./gradlew clean build
java -jar build/libs/plugpass-0.0.1-SNAPSHOT.jar
```

- 상태 확인: `http://localhost:8080/actuator/health`
- API 문서: `http://localhost:8080/docs/index.html`

API 문서는 테스트를 통과한 요청·응답으로 생성해 JAR에 포함한다.
생성 파일은 `build/docs/asciidoc/index.html`에서도 볼 수 있다.
개발 중 서버만 실행하려면 `./gradlew bootRun`을 사용한다. `/docs/index.html`은 패키징한 JAR에서 제공한다.

H2는 메모리 DB이므로 서버 종료 시 데이터가 사라진다.
T02~T13의 충전소 수집·저장·검색/상세·추천·반복 수집·장애 제한·운영 관측을 구현했다.
공공 API 키 없이 재현하는 검증은 합성 공급자 HTTP → 실제 H2 → 업무 HTTP 범위다. 실제 공공 API 인증은 아직 확인하지 못했다.
기본 설정은 수집 비활성화이므로 데이터 없는 조회가 가능하다. 설정과 운영 확인은 [운영 안내](docs/operations.md)를 따른다.

## 개발 기준

- [기술 스택과 책임 흐름](docs/official-stack-guide.md)
- [객체지향 생활 체조](docs/object-calisthenics.md)
- [기능·검증 범위](docs/feature-map.md)
- [작은 기능별 PR·자동 머지 절차](docs/delivery-workflow.md)
- [작업 지침](AGENTS.md)
- [agent-engineering 적용과 실행 근거](docs/agent-engineering-application.md)

빠른 반복은 `./gradlew test`, 공용 검증은 `bash scripts/verify.sh`다. 공용 검증은 동작·구조·필수 suite 실행과 JAR의 실제 HTTP를 확인하며 CI도 같은 명령을 사용한다. 테스트 보고서는 `build/reports/tests/test/index.html`에 생성된다.
현재 소스와 검증 로그를 묶는 [검증 계획](.agents/verification.json)은 전역 agent-engineering의 기존 증거 도구로 실행한다. [verify-plugpass](.agents/skills/verify-plugpass/SKILL.md)의 절차를 따른다.
