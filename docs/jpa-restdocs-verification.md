# JPA · REST Docs 검증

2026-10-01, Java 25에서 실행.

- `./gradlew clean build --console=plain`: 종료 0. 테스트 4개, 실패·오류·skip 0.
- JPA: H2 저장·flush·영속성 컨텍스트 clear 후 재조회. 저장 내용 일치와 새 객체임을 확인.
- REST Docs: status 필드 설명 누락 시 SnippetException으로 실패. 설명 복원 후 통과.
- 생성 HTML을 JAR의 클래스패스에 포함. 패키징 경로 교정 후 `./gradlew build` 종료 0.
- 최종 JAR 실행: health 200/UP, env 404, docs/index.html 200.
- 문서 HTTP 응답은 생성 HTML과 바이트 단위로 일치.
- 테스트 전용 엔티티가 실행 JAR에 없는 것을 확인.
- 직접 시작한 서버는 정상 종료 요청 후 graceful shutdown 확인.

## 범위

문서 생성은 MockMvc 테스트, 서버 응답은 별도의 실제 HTTP 호출로 검증했다.
H2는 메모리 방식이며 충전소 업무 엔티티·수집·추천 API는 아직 없다.
cmux 호출 환경과 live socket이 없어 화면 E2E는 수행하지 못했다.

## 남은 빌드 경고

Mockito 동적 agent, 문서 생성 엔진의 JDK native/Unsafe 호출, 문서 빌드 플러그인의 향후 Gradle 호환성 경고가 있다.
현재 빌드와 테스트는 통과했으며 버전 업그레이드 때 다시 확인한다.

## 로컬 증거

- build/test-results/test/TEST-*.xml
- build/docs/asciidoc/index.html
- build/verification/jpa-restdocs/ 아래 빌드·계약 실패·서버 로그 및 http-result.json

build 하위 결과는 clean 실행 시 지워진다. 재현 절차는 프로젝트 verify-plugpass를 따른다.
