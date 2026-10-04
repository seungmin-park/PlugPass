# CI 구조·누락 검사 실행 기록

날짜: 2026-10-05 (Asia/Seoul). 작업 공간: `/Users/seungmin/.codex/worktrees/ci-boundaries/PlugPass`, 브랜치 `codex/ci-boundaries`. 시작 main: `85d07bccb063615456bf0ef4b88a6edda38e76fd`. 추천 기능 작업이 있는 원래 checkout의 파일은 변경하지 않았다.

## 목표와 검증 책임

도메인 객체의 규칙·성공/실패 후 상태는 기존 동작 assertion이 확인한다. 구조 검사는 의존성과 공개 변경 경로의 형태를 확인한다. 객체의 실제 책임·이름·규칙 소유권은 [검증 기준](architecture-guardrails.md)과 PR 템플릿으로 리뷰한다. JaCoCo는 사용자가 제외를 선택했다.

## Red → Green

1. 기반 `./gradlew test --console=plain`: 기존 260개, 실패·오류·skip 0.
2. `python3 -m unittest discover -s scripts -p 'test_*.py' -v`: 다른 suite가 통과해도 필수 suite가 빠지면 거부해야 한다는 새 assertion이 `RuntimeError not raised`로 실패했다. 함수 인자 준비 중의 TypeError는 Red로 취급하지 않았으며, 인자 준비 후 실제 assertion 실패를 확인했다.
3. 최소 변경: 기존 XML 검사를 유지하며 요구 suite 이름과 실제 suite 이름의 차이를 거부한다. 신규 suite는 허용한다. 검사기 회귀 8건을 공용 verify 첫 단계에서 실행한다.
4. `./gradlew test --tests '*ArchitectureRulesTests' --console=plain`: no-op 검사기에서 16건 중 금지 구조 11건이 예외를 내지 않아 assertion 실패. 컴파일/환경 실패가 아닌 누락된 검사 동작의 Red다.
5. 최소 변경: ArchUnit core 1.5.1, 구조 규칙 6개, 허용/금지 예제 16개. `./gradlew test --tests '*Architecture*Tests' --console=plain`: 22건 통과.

## 실제 위반의 거부

- `StationReadFlowTests`의 XML 하나를 임시 제거하고 `python3 scripts/verify-runtime.py`: 종료 1, `Missing required test suites: com.plugpass.search.StationReadFlowTests`. 다른 테스트가 남아 있어도 거부했다. XML은 finally에서 복원했다.
- 실제 StationController에 StationRepository 필드를 임시 추가하고 `./gradlew test --tests 'com.plugpass.architecture.ArchitectureTests.controllersUseServiceContracts' --console=plain`: 종료 1, Architecture Violation에 실제 StationController→StationRepository 의존성이 명시됐다. 소스는 finally에서 복원했다.
- raw probe 로그는 공용 전체 검증 후 `build/verification/guardrails/`에 보관한다. 위반은 실제 네트워크·DB 업무 동작을 바꾸지 않는 임시 구조 입력이었다.

## 전체 검증과 환경

`JAVA_HOME=/Users/seungmin/.asdf/installs/java/zulu-25.36.205/Contents/Home bash scripts/verify.sh`에서 Python 8건, Java 282건·실패/오류/skip 0, clean build, 문서 생성·JAR 일치, health 200 UP·세부 정보 없음, env 404, 빈 검색 200, validation 400, 없는 상세 404를 확인했다. 임시 위반 복원 후 같은 공용 명령을 다시 실행한다. 최종 XML·runtime.json·server.log가 실제 결과의 근거다. 원격 CI 결과는 해당 PR의 checks에서 별도로 확인한다.

처음 두 공용 실행은 테스트·빌드가 통과했으나 JAR 실행이 Java 21/class version 65로 실패했다. Gradle toolchain은 Java 25/class version 69였다. macOS java_home은 asdf JDK 25를 찾지 못했으므로 `./gradlew javaToolchains`로 실제 경로를 확인하고 명시했다. 이를 검사 실패나 통과로 바꾸지 않았다. 기존 Gradle 10 호환성 deprecation 및 Lombok/JVM 경고는 있었으나 테스트 assertion과 빌드는 통과했다.

cmux 호출 환경 변수와 살아 있는 socket이 없어 identify/ping/capabilities가 실패했다. 로컬 검증은 터미널 runner 및 loopback HTTP 범위이며, 화면에 보이는 cmux E2E로 보고하지 않는다.

## 책임·이름·실패 조건 리뷰

- ArchitectureRules는 검사 정책만 소유한다. ArchitectureTests는 실제 운영/테스트 bytecode를 가져오고, ArchitectureRulesTests는 금지/허용 입력으로 검사기의 판정 결과를 확인한다. 업무 도메인 로직을 이 검사기로 옮기지 않았다.
- 명시 타입·import를 사용했다. 검사 함수 이름은 검사 대상을 드러내며, fixture는 구조 입력으로만 쓰고 앱의 component/entity scan 밖에 뒀다. 일반 도메인 객체 준비용 묶음 fixture와 구분한다.
- JPA 애너테이션과 읽기 getter, MVC의 Service mock은 허용한다. Service 통합 테스트의 트랜잭션·Mockito 의존은 거부한다. 별도 source 규약 검사기를 선구현하지 않았다.
- 기존 도메인 예외·좌표/시각 경계·중복·수집 rollback·외부 오류/timeout·HTTP validation·실제 fixture 연결 테스트 260건을 유지했다. 외부 장애·DB 상태의 새 규칙을 만든 작업이 아니므로 새로운 동시성·운영 API·성능 시나리오는 적용하지 않았다.
- 운영 코드는 최종 diff에 없다. HTTP JSON·경로·DB 컬럼·트랜잭션 계약은 바꾸지 않았다. 구조 통과로 semantic 책임 전체나 요구사항 완전성을 주장하지 않는다. 독립 병렬 에이전트 리뷰는 실행하지 않았다.
- Refactor: 금지 Spring/JPA 예제를 test-only `architecturefixture` 패키지로 분리해 앱 스캔과 격리했다. 규칙 선택과 판정은 작은 private predicate로 공유했다. 외부 I/O나 테스트 대상 도메인의 실제 규칙을 fake로 대체하지 않았다.
