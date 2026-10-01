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
현재는 JPA 연결·저장·재조회와 상태 확인 API까지 검증했다. 충전소 수집·추천 API는 아직 없다.

## 개발 기준

- [기술 스택과 책임 흐름](docs/official-stack-guide.md)
- [객체지향 생활 체조](docs/object-calisthenics.md)
- [기능·검증 범위](docs/feature-map.md)
- [작업 지침](AGENTS.md)

`./gradlew test`로 HTTP·JPA·문서 계약을 검증한다. 테스트 보고서는 `build/reports/tests/test/index.html`에 생성된다.
