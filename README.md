# PlugPass · 플러그패스

전기차 충전소의 상태와 정보 최신성을 확인하고 대체 후보를 찾는 웹앱.

## 기술 스택

- Java 25 LTS
- Spring Boot 4.1.1
- JPA · H2
- Spring REST Docs

## 실행

JDK25·Node26.7.0 환경에서:

```bash
./gradlew clean build
java -jar build/libs/plugpass-0.0.1-SNAPSHOT.jar
```

- 상태 확인: `http://localhost:8080/actuator/health`
- API 문서: `http://localhost:8080/docs/index.html`
- 웹앱: `http://localhost:8080/app/index.html#/stations`

API 문서는 테스트를 통과한 요청·응답으로 생성해 JAR에 포함한다.
생성 파일은 `build/docs/asciidoc/index.html`에서도 볼 수 있다.
개발 중 서버만 실행하려면 `./gradlew bootRun`을 사용한다. `/docs/index.html`은 패키징한 JAR에서 제공한다.

H2는 메모리 DB이므로 서버 종료 시 데이터가 사라진다.
T02~T15의 충전소 수집·저장·검색/상세·추천·반복 수집·장애 제한·운영 관측·종합 HTTP 데모·성능 기준선을 구현·검증했다.
공공 API 키 없이 재현하는 검증은 합성 공급자 HTTP → 실제 H2 → 업무 HTTP 범위다. 실제 공공 API 인증은 아직 확인하지 못했다.
기본 설정은 수집 비활성화이므로 데이터 없는 조회가 가능하다. 설정과 운영 확인은 [운영 안내](docs/operations.md)를 따른다.

## 4주차 결과와 재현

[데모](docs/demo.md)는 공급자 fixture HTTP 수집→실제 저장→주변 검색→상세→첫 ID 제외 추천과 중복·STALE·503·복구·빈 결과를 재현한다. [성능 기록](docs/performance.md)은 생성 충전소10,000/충전기50,000·동시20명·30초 준비/180초 측정의 원본과 명령을 제공한다.

측정4591요청·오류0%, 전체 p50 768.01ms·p95 1425.41ms로300ms 목표는 미달이다. 검색/추천은 SQL2개지만60001엔티티를 매번 로딩하며 T16에서 개선 전후를 비교한다. H2·고정 합성 데이터·로컬 단일 JVM 범위이며 운영 성능이나 실제 공급자 최신성을 보장하지 않는다. 당시 cmux live socket이 없어 화면 E2E는 미확인이었다. 현재 검증은 아래 T23/T24 기록을 따른다. 전체 Java357·Python14 실패/오류/skip0과 PR/CI/main 근거는 [4주차 기록](docs/week4-verification.md)·[체크리스트](tasks.md)를 따른다.

## 개발 기준

- [기술 스택과 책임 흐름](docs/official-stack-guide.md)
- [객체지향 생활 체조](docs/object-calisthenics.md)
- [기능·검증 범위](docs/feature-map.md)
- [작은 기능별 PR·자동 머지 절차](docs/delivery-workflow.md)
- [작업 지침](AGENTS.md)
- [agent-engineering 적용과 실행 근거](docs/agent-engineering-application.md)

빠른 반복은 `./gradlew test`, 공용 검증은 `bash scripts/verify.sh`다. 공용 검증은 동작·구조·필수 suite 실행과 JAR의 실제 HTTP를 확인하며 CI도 같은 명령을 사용한다. 테스트 보고서는 `build/reports/tests/test/index.html`에 생성된다.
현재 소스와 검증 로그를 묶는 [검증 계획](.agents/verification.json)은 전역 agent-engineering의 기존 증거 도구로 실행한다. [verify-plugpass](.agents/skills/verify-plugpass/SKILL.md)의 절차를 따른다.

## 웹앱 개발

Node26.7.0에서 `npm --prefix frontend ci`, `npm --prefix frontend run dev` 후 `http://localhost:5173/app/index.html`을 연다. Vue·TypeScript로 위치 선택→검색→상세→대체 후보→다른 상세를 제공한다. 개발 서버의 `/api` 요청은 localhost8080의 Spring 서버에 전달한다. Gradle build가 타입 검사 후 웹앱을 빌드해 Spring JAR의 static/app에 포함한다. 배포 JAR은 웹앱·API·REST Docs를 같은 origin에서 제공한다.

프런트 단위 테스트·타입·lint·build는 [frontend 사용법](frontend/README.md), 실제 Red/Green·브라우저·제한은 [실행 기록](docs/frontend-verification.md)을 따른다. 공용 `bash scripts/verify.sh`는 Node와 Java/Python 환경을 함께 사용한다.

`bash scripts/frontend-e2e.sh`는 합성 공급자 HTTP→실제 수집/H2/API와 개발 모드의 브라우저 흐름을 재현한다. `--serve`는 cmux 직접 클릭 검증용으로 서버를 유지한다.
공용verify는 개발모드 E2E17건과 배포JAR 브라우저2건을 모두 실행한다. 로컬은 현재cmux에서 headed로, CI는 headless로 실행한다. 최초 로컬 Chromium 설치는 `cd frontend && npx playwright install chromium`이다.
375×812·1440×900, 키보드·오류입력 연결/포커스와 필수 suite 누락·0개·skip 거부의 근거는 [T24 실행기록](docs/t24-verification.md)을 따른다.
