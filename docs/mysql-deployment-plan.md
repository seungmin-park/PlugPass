# MySQL 운영 DB 전환 계획

> 실행: `superpowers:executing-plans`로 현재 세션에서 순차 구현한다. 사용자 확정: 2026-10-12 MySQL. 기존 `plan.md`·`tasks.md`의 배포 후속 범위를 구체화하며 공개 배포·클라우드 자원 생성·병렬 에이전트 실행을 포함하지 않는다.

**목표:** 배포 프로필을 MySQL 8.4 LTS로 전환하고 실제 저장·조회·재시작 보존을 확인한다.

**구조:** Controller·Service·Domain·Repository의 업무 책임과 HTTP 계약은 유지한다. Flyway가 버전별 MySQL 스키마를 생성하고 Hibernate는 `validate`로 매핑을 검사한다. 기본 개발/단위·통합 회귀는 H2를 유지하고 별도 실제 MySQL 검증을 공용 verify와 CI에 필수로 연결한다.

**스택:** Java25, Spring Boot4.1.1, JPA·MySQL(운영)/H2(개발·기존 테스트), Spring REST Docs.

## 경계와 실패 조건

- DB 주소·암호는 환경변수로 받는다. 배포 프로필에 H2 대체 주소를 두지 않는다.
- 수집 서비스는 기존 페이지별 트랜잭션을 유지한다. DB 전환용 서비스/Repository 추상화를 새로 만들지 않는다.
- UTF-8 한글·이모지, 식별자 대소문자/선행0, UTC 마이크로초 시각을 실제 MySQL로 검증한다.
- 기존 데이터가 있는 비관리 스키마의 자동 baseline·테이블 자동 수정·데이터 덮어쓰기를 하지 않는다. 기존 H2 데이터 이관은 대상 데이터가 제공되지 않은 별도 작업이다.
- DB 연결 실패·마이그레이션/매핑 불일치 시 기동 실패를 확인한다. 공급자 장애·재시도·잘못된 요청의 기존 회귀도 유지한다.
- 로컬 E2E는 `/Users/seungmin/Desktop/repo/PlugPass`의 현재 cmux 호출 workspace에 표시한다. Docker 사용 불가는 통과/skip으로 처리하지 않는다.

## T26: MySQL 연결·버전 스키마·저장 계약

파일: `build.gradle`, `application*.properties`, `db/migration/mysql/V1__initial_schema.sql`, `src/test/java/mysql/MySqlDeploymentTests.java`, `scripts/mysql_environment.py`, `scripts/verify-mysql.py`.

- [x] 테스트 전용 Docker 환경과 `mysqlTest` 실행 경계를 준비한다. 임의 이름·임시 인증정보·loopback 포트로 다른 DB와 격리한다.
- [x] Red: `python3 scripts/verify-mysql.py --tests '*MySqlDeploymentTests.migratesAndPreservesDataAcrossApplicationRestart' --tests-only`로 실제 MySQL 배포 기동·저장·재기동 assertion 실패를 확인한다. 환경 오류는 Red로 세지 않는다.
- [x] Green: Boot 관리 MySQL Connector/J·Flyway starter/mysql 모듈, 배포 연결 설정과 V1 스키마를 추가한다. 기본 H2에서는 Flyway를 비활성화한다.
- [x] 동일 ID 재수집·대소문자 ID·한글/이모지·UTC 마이크로초·child 저장 실패 rollback·unique/FK·스키마 불일치를 실제 DB로 검증한다.
- [x] 대상과 전체 Java 테스트를 확인한다. 이름·책임·호출부·HTTP 계약을 리뷰하고 불필요한 구조 변경을 피한다.

## T27: 실제 재시작 흐름·공용 검증·전달

파일: `scripts/verify-mysql.py`, `scripts/verify.sh`, `.github/workflows/verify.yml`, `.env.example`, `README.md`, `docs/operations.md`, `docs/feature-map.md`, 공식 출처/학습 기록, 실행 기록.

- [x] 합성 공급자 HTTP→실제 수집/MySQL→검색/상세/추천을 확인한다. 앱 종료·MySQL 컨테이너 재시작 후 재수집 없는 별도 운영 JAR JVM에서도 데이터/완료 이력이 보존되어야 한다.
- [x] MySQL 테스트 XML의 0개·실패·오류·skip을 거부한다. MySQL 검증을 기존 공용 verify와 필수 CI에 연결한다.
- [x] 같은 cmux workspace의 브라우저에서 실제 위치/조건 입력→검색→상세→대체 후보를 조작한다. 기존 headed 개발/JAR E2E도 유지한다.
- [x] `bash scripts/verify.sh` 종료0, 실제 XML/JSON·HTTP 결과를 확인하고 소스/환경/명령/한계를 기록한다.
- [ ] 문서/환경변수 예시를 갱신하고 서명 commit/push→PR→보호 규칙/필수 CI→native auto-merge→실제 main 머지를 확인한다.

## 계획 검토

스키마는 기존 세 업무 엔티티의 필드·unique·FK·enum·version을 보존한다. 기본 MySQL collation이 Java 식별자 비교를 바꾸는 조건, UTC 복원, 미관리/손상 스키마, DB 중단, 재시작 후 준비 상태를 별도로 검토한다. 공개 API·프런트 기능·공급자 timestamp 해석·기존 성능 목표는 변경하지 않는다. H2 기준 성능을 MySQL 성능으로 주장하지 않는다.
