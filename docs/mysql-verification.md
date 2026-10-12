# MySQL 전환 실행 기록 — 2026-10-12

사용자가 운영 DB를 MySQL로 확정했다. 기존 파일 H2 배포 프로필을 MySQL8.4 LTS·Flyway로 전환했다. 기본 H2 개발·기존 회귀, Service/Domain/Repository 책임과 공개 HTTP 계약을 유지한다. [계획](mysql-deployment-plan.md)·[운영 설정](operations.md#mysql-배포-프로필)·[기능 지도](feature-map.md)를 따른다.

## 왜 이 구성이 필요한가

```text
처음 기동: 빈 MySQL 스키마 → Flyway V1 → Hibernate validate → HTTP 준비
재기동:    기존 스키마/이력 → Flyway 검사 → Hibernate validate → 저장 데이터 조회

수집 서비스 ── 페이지 transaction ──► JPA/Repository ──► MySQL
조회 서비스 ── 기존 검색/상세/추천 ────────────────────┘
```

Flyway는 DB 구조의 버전을 소유하고 JPA는 객체/SQL 매핑을 소유한다. 업무 Service가 MySQL 설치·DDL·접속 옵션을 알 필요는 없다. 기존 transaction의 commit/rollback을 MySQL에서 검증하므로 DB 전환용 Service/Repository 래퍼는 추가하지 않았다. Hibernate 자동 생성/삭제 대신 validate를 사용해 재시작 시 업무 데이터를 유지한다.

## Red → Green → 검토

1. **배포 연결 Red:** 실제 격리 MySQL 준비 후 `python3 scripts/verify-mysql.py --tests '*MySqlDeploymentTests.migratesAndPreservesDataAcrossApplicationRestart' --tests-only`. 테스트1·assertion 실패1·오류/skip0. 기존 H2 드라이버 배포 설정이 MySQL JDBC metadata를 얻지 못했다. [근거](evidence/mysql/connection-red.txt).
2. **Green:** Boot관리 Connector/J와 Flyway starter/mysql 모듈, 필수 DB URL/암호, UTC 연결, MySQL V1 스키마를 추가했다. H2 기본 프로필은 Flyway 비활성화다. 초기 Green 실행에서 테스트 전용 PersistenceProbe까지 스캔해 없는 테이블을 요구했다. 테스트 기동의 EntityScan/Repository 범위를 실제 업무 패키지로 제한했다. 이 준비 오류를 기능 Red로 세지 않았다. 운영 JAR은 이 테스트 설정 없이도 같은 V1으로 기동한다.
3. **검사기 Red:** mysql/ 테스트 클래스가 운영 JAR에 들어간 예제를 기존 검사기가 거부하지 못했다. `test_rejects_mysql_test_configuration_in_jar`의 `RuntimeError not raised`를 확인하고 검사 범위를 확장했다. 기존 정상 운영 클래스 허용도 유지한다.
4. **환경 격리 Red:** `SPRING_DATASOURCE_URL`·Flyway URL·Spring JSON·JVM system property가 부모 셸에서 유입되는 조건을 작성했다. 환경 경계 assertion1 실패 후 해당 애플리케이션/비밀 설정을 제거하고 PATH·CI 같은 실행 기반만 유지했다. 부모 환경은 변경하지 않는다. [검사기 근거](evidence/mysql/guards-red.txt).
5. **Refactor/유지:** 컨테이너 수명·인증정보·환경 격리는 `mysql_environment.py`, 실제 저장/JAR/HTTP 실행은 `verify-mysql.py`, 업무 규칙은 기존 객체가 소유한다. 환경 복사를 작은 함수로 추출해 외부 DB로 향할 수 없는 경계를 검사했다. Controller·DTO·도메인·프런트 상태/API는 변경하지 않았다. 기존 브라우저 journey를 재사용하고 DB와 무관한 테스트 제목으로 고쳤다.

서버 strict mode를 끈 조건에서도 처음부터 롤백 테스트가 통과했다. Connector/J9.7.0이 세션에 STRICT_TRANS_TABLES를 적용하는 기존 동작이었다. 이를 새 Red로 보고하지 않았고, global은 비엄격/session은 strict임을 함께 확인한다. 중복 세션 초기화 SQL은 추가하지 않았다.

## 실제 검증 범위

| 실행 | 관측 결과 |
| --- | --- |
| Java 전체 `./gradlew test --rerun-tasks` / 공용 clean build | 372건, 실패/오류/skip0 |
| 실제 MySQL `MySqlDeploymentTests` | 7건, 실패/오류/skip0 |
| Python 전체 검사기 | 34건, 실패0 |
| 프런트 단위 / 타입 / lint / build | 290건(27파일), strict type·경고0 lint·build 통과 |
| headed 개발 모드 Playwright | 21건 통과 |
| 기본 JAR Playwright | 3건 통과 |
| 재시작 MySQL의 운영 JAR Playwright | 기존 journey1건 통과 |
| 메모리 H2 별도 JVM3회 | 기존 데이터 소실·준비 초기화·재수집 회복 유지 |
| MySQL 별도 수집 JVM→DB 재시작→운영 JAR JVM | 검색/상세/추천 JSON 동일, 데이터·완료 이력·dataReady 보존 |
| DB 설정 없는 deployment JAR | 비정상 종료, MySQL URL 누락을 H2로 대체하지 않음 |

MySQL은 공식8.4 이미지의 digest `sha256:6ea90827b1100f8f2ae306a539f86d2c264a26ed435a2a9f75551dd5c3aeb242`, 실제 서버 **8.4.11**이다. Gradle 해석은 Connector/J9.7.0·Flyway12.4.0·Hibernate7.4.5.Final·Boot4.1.1, JDK25.0.4.1·Node26.7.0이다. 시각 테스트 JVM은 Asia/Seoul이고 DB 연결/저장은 UTC다.

MySQL7건은 한글/이모지·UTC 마이크로초·null 관측 시각·앱 재시작/이력·대소문자/끝 공백/선행0 식별자·같은 ID 재수집·child 저장 실패 rollback·unique/FK·스키마 drift 기동 거부를 확인한다. 스키마 drift는 테스트가 변경한 컬럼을 finally에서 복원한다. Service 호출 종료/commit 뒤 별도 Repository로 읽고 자식→부모→수집 이력 순서로 정리한다. 운영 DB나 seed SQL에 의존하지 않는다.

초기 재시작 검증은 Docker Desktop이 임시 호스트 포트를 재할당했는데 이전 URL을 사용해 실패했다. 별도 probe에서 `56456→56470`을 확인했다. 재시작 뒤 `docker port`로 URL을 갱신하고 내부 TCP 준비를 확인한 후 재검증했다. 앱의 재시도/접속 오류를 숨겨 통과시키지 않았다.

## cmux 실제 사용자 흐름

환경 격리 수정 뒤 공용 verify가 종료0이었다. 마지막 검토에서 충전소 중복·충전기 중복·FK 삭제 실패를 각각 독립 테스트로 분리해 MySQL5→7건으로 정리했다. 제품 코드는 추가 변경하지 않았고, 분리 후 MySQL7건과 별도 JVM/배포 JAR/브라우저1건을 다시 통과했다. 이 제약 테스트들은 이미 구현된 DB 계약 확인이며 새로운 기능 Red로 세지 않는다.

호출 환경과 `cmux identify --json`으로 workspace:1000030004·호출 surface:1000030011을 확인했다. 화면에 선택된 다른 workspace는 변경하지 않았다. 오른쪽 보조 pane:1000030012의 terminal surface:1000030022에서 `/Users/seungmin/Desktop/repo/PlugPass`의 러너/서버/결과를 표시했다. 같은 pane의 browser surface:1000030028을 생성했다. 기존 pane이 없어 한 번만 만들었으며 생성은 `--focus false`였다.

`python3 scripts/verify-mysql.py --serve`가 합성 공급자 HTTP로 두 충전소를 실제 수집하고 MySQL을 재시작했다. 이후 **일반 운영 JAR**의 `http://127.0.0.1:58024`에 접속해 예시 위치 선택→검색2곳→첫 상세ID9→excludeStationId9 후보→다른 상세ID10/browser_second를 실제 클릭했다. 검색 건수·관측 시각 확인 불가·첫 충전소 링크 부재·두 번째 ID/공급자 ID를 DOM assertion으로 확인했다. console/errors 목록은 비어 있었다.

CLI의 surface-bound browser 명령은 `--workspace`를 지원하지 않아 한 번 거부됐다. 생성 시 확인한 workspace 소속의 정확한 `--surface`로 진행했다. 다른 pane으로 이동하거나 포커스를 바꾸지 않았다. Playwright는 별도 실제 headed Chromium이며 cmux 브라우저 직접 조작과 구분한다. 기존 실패 화면/타일 제어 E2E는 H2 개발 모드 범위를 유지하고, MySQL JAR에서는 실제 핵심 journey를 검증했다.

자신이 만든 앱·컨테이너/임시 데이터만 종료했고 pane과 브라우저는 유지했다. 로그/원본: `build/verification/mysql/`, `build/verification/mysql-manual/`, `build/test-results/mysqlTest/`, `frontend/test-results/mysql.json`. 저장소의 [로컬 요약](evidence/mysql/local-summary.json)은 소스 hash와 실제 JSON/보고서 요약을 연결한다. 비밀값·실공공 API 키는 수집하지 않았다.

## 리뷰와 한계

이름→실제 동작→책임 배치를 검토했다. 운영 스키마는 세 업무 엔티티만 포함하고 테스트 애플리케이션은 test classpath에만 둔다. Service/Repository 유스케이스 계약·응답 JSON·라우트는 유지한다. 문자열 비교/시각/DB 제약은 매핑 경계에서 실제 검증하고 잘못된 데이터의 도메인 판단을 Flyway로 옮기지 않았다.

사용자의 병렬 에이전트 권한 제한에 따라 작성자가 별도 diff 리뷰를 수행했다. 독립 리뷰어의 검토는 아니다. 발견한 환경 격리 문제는 Red→Green과 전체 Python·실제 MySQL/JAR 재검증으로 수정했다. 외부 API timeout/인증/429/5xx와 도메인 경계는 기존 회귀를 유지한다. MySQL에서 모든 기존 H2 테스트를 다시 실행했다고 주장하지 않는다.

기존 H2 파일 데이터의 자동 이관은 없다. 실제 운영 DB/계정·TLS·백업/복원·배포 대상과 네트워크 구성, 전체 실공공 데이터 수집·한도, MySQL 부하 성능은 별도 운영 준비다. 이번 결과는 코드와 로컬 MySQL 배포 프로필의 검증이며 공개 배포 완료가 아니다. 원격 필수 CI·실제 머지는 전달 근거에서 별도로 확인한다.

## 원격 전달 완료

서명 head `c2332c80a2284e316096fb29cf6e5f879bd02c5a`를 GitHub가 valid로 확인했다. [PR #52](https://github.com/seungmin-park/PlugPass/pull/52)의 [필수 CI](https://github.com/seungmin-park/PlugPass/actions/runs/38239046683)는 성공했고, 원본 artifact의 JUnit XML에서 기본372·MySQL7, 실패/오류/skip0을 직접 확인했다. runtime JSON은 프런트290·개발 브라우저21·기본 JAR3이며 MySQL 결과는 별도 JVM2·DB 재시작·데이터/준비 상태 보존·JAR journey1·DB 설정 누락 거부다. [원본 보고서 hash와 요약](evidence/mysql/ci-summary.json)에 연결한다.

main의 strict 필수 검사 `PlugPass verify`(GitHub Actions 앱15368), 관리자 적용·강제 push/삭제 금지·PR 필수와 native auto-merge/squash를 조회했다. 보호 규칙 아래 `--auto --squash --match-head-commit`으로 전달했고, 실제 머지는 2026-10-12 10:58:51 UTC, main `36f8f3a093ecfc426052816c4f1a3dde2f5fdb56`이다. 로컬 main도 fast-forward로 동기화했다. T26/T27 완료 표시는 이 관찰에 근거하며 공개 배포 완료를 뜻하지 않는다.
