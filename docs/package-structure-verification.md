# 패키지 구조 정리 검증

2026-10-05. 작업 브랜치 `codex/package-structure`, 기준 main `6c40b0b`.

## 변경과 책임 검토

- 사용자 요구: 기존 기능별 묶음을 유지하며 dto·domain·repository·service를 구분한다.
- Java 파일 79개를 역할별로 이동하고 package/import와 호출부를 갱신했다. Controller는 controller, HTTP DTO는 dto.request/dto.response에 둔다. 테스트도 대상 역할을 따르고 연결 흐름은 integration에 둔다.
- Connector는 검색 전용 개념이 아니어서 station.domain, 두 요청이 공유하는 FiniteDouble은 common.validation으로 옮겼다. 내부 SyncProgress는 service의 package-private 구현으로 유지한다.
- XML 응답 record의 접근 범위는 client에서 사용할 수 있도록 public으로 조정했다. 상태 정규화는 client가 수행하고 DTO에 ChargerStatus를 전달한다. DB 매핑·트랜잭션·정렬·JSON 필드·validation 메시지·URL은 유지한다.
- package/import를 제외한 전후 소스 비교에서 변경은 PublicDataItem의 접근 범위·상태 인자, PublicDataResponse 접근 범위, client의 상태 매핑 전달뿐이다. 테스트 본문의 assertion은 변경하지 않았다.
- 역할에 맞는 기존 이름은 유지했다. 줄 수를 맞추기 위한 클래스·인터페이스·라이브러리는 추가하지 않았다. 프런트엔드·TypeScript는 적용 대상 없음. 패키지 이름만으로 의존성을 자동 강제한다고 보고하지 않는다.

## 테스트 순서와 결과

이번 변경은 신규 동작 없는 Refactor다. 기존 동작이 맞는 상태를 먼저 실행했고, 일부러 기존 assertion을 깨뜨려 기능 Red라고 기록하지 않았다. Green 구현을 새로 만드는 작업은 적용 대상 없음.

1. 변경 전 `./gradlew test`: 287 tests, failures/errors/skipped 0, 종료 0.
2. 이동 후 `./gradlew test`: 같은 287 tests, failures/errors/skipped 0, 종료 0.
3. 최초 `bash scripts/verify.sh`: build와 287 tests는 성공, JAR 시작은 UnsupportedClassVersionError로 종료 1. 기본 터미널 Java가 21이고 JAR은 Java 25로 컴파일됐기 때문이다. 이 실패는 기능 Red도 HTTP 검증 통과도 아니다.
4. Gradle javaToolchains로 빌드 JDK 경로를 확인하고 아래 명령으로 동일 JDK를 지정했다. `JAVA_HOME`은 이 명령에만 적용하고 전역 설정은 바꾸지 않았다.

```bash
JAVA_HOME=/Users/seungmin/.asdf/installs/java/zulu-25.36.205/Contents/Home bash scripts/verify.sh
```

결과: 종료 0, clean build 성공, 287 tests, failures/errors/skipped 0. 다음 실행 JAR HTTP assertion을 모두 확인했다.

| 실제 경계 | 결과 |
| --- | --- |
| health | 200/UP, components 없음 |
| env | 404 |
| 생성 문서 → JAR 내부 → HTTP 문서 | 내용 동일, HTTP 200 |
| 초기 검색 | 200, dataReady=false, 빈 stations |
| 초기 추천 | 200, 세 그룹 모두 빈 목록 |
| 잘못된 좌표 | 400/INVALID_REQUEST |
| 존재하지 않는 상세 | 404/STATION_NOT_FOUND, 공개 메시지·fields 동일 |
| 테스트 전용 persistence fixture | 실행 JAR에 미포함 |

실제 공급자 fixture HTTP→수집→H2→검색/상세, fixture HTTP→추천 HTTP는 기존 integration 테스트가 담당한다. 도메인 입력 경계·동일 식별자·잘못된 갱신 후 상태 보호, DB unique/commit/rollback, 페이지/외부 오류/timeout, 최신성 시간 경계·빈 결과·HTTP validation·404·후보 제외를 기존 287개 테스트로 재확인했다. 신규 네트워크 재시도·운영 공공 API 실인증·성능 부하는 이번 구조 변경의 동작 범위가 아니다.

## 화면 표시와 증거 위치

CMUX_WORKSPACE_ID·CMUX_SURFACE_ID는 미설정이다. cmux identify --json은 No live cmux socket found로 종료했다. 따라서 cmux 화면 검증은 수행하지 못했고, 일반 실행 환경의 실제 HTTP·JUnit assertion으로 검증했다.

재현 시 생성되는 결과: build/test-results/test/TEST-*.xml, build/reports/tests/test/index.html, build/verification/runtime.json, build/verification/server.log, build/verification/station-read-flow/*.json, build/verification/package-structure.log. 초기 JDK 실패 로그는 build/verification/package-structure-jdk21-failure.log에 별도 보존한다. 검증 서버는 직접 시작한 프로세스만 종료했다.

## 최신 main의 T11 통합

구조 정리 도중 main에 반영된 `8607e46`(PR #20)을 적용했다. SKIPPED 상태·실행 소유권·스케줄 설정은 그대로 유지하고 새 Java 파일 4개도 역할별로 이동했다. 최종 이동은 83개다. feature map·verify skill은 T11 설명과 패키지 설명을 모두 유지했다. 최종 JDK 25 공용 검증은 T11의 6개 테스트를 포함한 293개에서 failures/errors/skipped 0, clean build·실제 HTTP assertion 통과, 종료 0이다.

T11 반영 직후 compileJava에서 새 SKIPPED 참조의 SyncStatus import 누락을 발견했다. enum은 이동돼 있었으며 import를 보완했다. 컴파일 실패는 기능 Red로 계산하지 않고 공용 검증을 재실행했다.

최신 main 기준으로 다시 비교해 모든 이동 테스트 본문이 동일함을 확인했다. 역할별 파일 경로와 package 선언은 모두 일치하고 갱신한 문서의 로컬 링크도 유효하다. 최종 코드 검증 이후의 추가 변경은 이 실행 기록뿐이다.
