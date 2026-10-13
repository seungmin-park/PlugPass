# 운영 데이터 준비 검증 기록

## T33/T34 기반 (2026-10-13)

기준 main 0fbe835. 사용자 process로 초기 실수집·보존·갱신·백업을 진행한다. API별 일일100,000회는 사용자 계정 화면 확인값이다.

- Red: 실제 MySQL의 별도 연결이 GET_LOCK을 가진 상태에서 기존 synchronize가 SKIPPED 대신 FAILURE를 반환했다. `/tmp/plugpass-production/lock-red.log` assertion 실패를 확인했다. 이력0·요청0 및 연결 종료 후 재진입도 함께 검증한다.
- Green: IngestionLock 외부I/O 경계와 DB 세션 잠금, 해제·연결 반환/abort, H2 기존 JVM 소유권을 구현했다. 실제 MySQL8건 종료0 (`lock-green.log`). 운영 단일VM 실행 잠금과 함께 쓰며 분산DB 합의/fencing을 보장한다고 표현하지 않는다.
- Red: ALL 지역과200페이지/300요청/30분 예산을 기존 구성자가 거부하는 assertion2건 (`national-red.log`). Green: ALL은 zcode 생략, 기존 지역은 유지하고 확장 예산도 유한 상한을 가진다. 기본값10/20/120초는 유지한다. 기존 invalid-budget 테스트의 기존 상한+1 사례를 새 상한+1로 변경한 것은 요구사항 변경에 따른 것으로 기록한다.
- Red: 일회 실행 옵션에도 ApplicationRunner가 없는 assertion 실패 (`command-red.log`). Green: 명시적 command만 기동 즉시1회, 성공0/실패1/중복2, 기본기동 비실행. 성공·인증실패·기본비실행을 실제 서비스/DB로 검증했다.
- Refactor/책임: DefaultStationSyncService는 흐름과 페이지 commit 협력, DatabaseIngestionLock은 외부 DB 소유권, IngestionCommand는 CLI 실행/종료 코드를 맡는다. DB 잠금은 네트워크 대기 중 transaction을 열지 않는다. 비밀/원문 예외를 추가 로그로 노출하지 않는다. 기존 HTTP·DB 스키마 변경 없음.
- 공용 `bash scripts/verify.sh` 종료0: Java428·프런트350·Python32·개발브라우저30·빈DBJAR4·실MySQL8·재시작MySQL/JAR7. 실패/오류/skip0, 타입/lint/build 및 실제 데이터 보존. `/tmp/plugpass-production/phase1-verify.log`, `.exit`, `build/verification/runtime.json`, `build/verification/mysql/result.json`.
- 현재 cmux caller workspace1000050003, 검증 terminal surface1000050012, browser surface1000050013을 재사용했다. 사용자의 포커스가 다른 workspace여도 호출 workspace를 기준으로 실행했고 임의 포커스를 변경하지 않았다. 실제 JAR CLI 인증실패 종료 검증은 `packaged-command.log`·`.exit`로 별도 기록한다.
- 공급자 VM 읽기 probe 각1회: getChargerInfo HTTP200/code00, total527178/page9999/9.7MB/1.44초; getChargerStatus period10 HTTP200/code00, total13103/page9999/2.2MB/0.48초. 첫 페이지일 뿐 전체 수집 성공이 아니다. 키·원문 응답은 저장하지 않았다.

남은 작업: 운영 초기 전체 수집, 상태API·갱신 안내, 운영 실행 스케줄·백업 복원·최종 공개 흐름. 이 기반의 CI/배포만으로 전체 작업 완료로 판정하지 않는다.

## 전국 초기 수집에서 확인한 저장 병목

PR #57 main1e7b91b의 CI38368626850·CD38369290604 성공 후 같은 JAR로 운영 최초 수집을 실행했다. 공급자 첫 페이지는 빠르게 반환됐지만9,999건을 저장하며 JVM CPU94%를 사용했다. 비종료 SIGQUIT 스레드 덤프의 main은 Hibernate autoFlush→checkForTransientReferences→Repository 단건 조회 경로였다. `/tmp/plugpass-production/initial-live.log`·`initial-thread-dump.log`. 운영 MySQL 읽기에서 당시 charger0/run1을 확인했고 이번 작업이 만든 수집 컨테이너만 정상 종료했다(143). 웹·DB는 유지했다. 수집 전체 성공으로 기록하지 않는다.

Red: 20개 기존 충전기를 같은 페이지로 다시 저장할 때 Hibernate query 실행40회, 상한2 assertion 실패(`page-efficiency-red.log`). Green: 페이지의 충전기 식별자와 충전소 목록을 먼저 읽고 map에서 기존 객체를 찾아 갱신해 조회2회 이내로 제한했다. 동일페이지 중복 객체와 관측 시각 거부·페이지 rollback은 기존 서비스/수집 테스트로 함께 확인했다. 단건 upsert 계약은 유지한다. IDENTITY insert를 임의 flush/clear로 숨기거나 bulk SQL로 도메인 규칙을 우회하지 않는다. 실제 전국 완료 시간은 재배포 후 재수집으로 별도 확인한다.

저장 병목 수정 공용 verify 종료0: Java429·프런트350·Python32·개발30·빈DBJAR4·MySQL8·재시작JAR7. 실패/오류/skip0. `/tmp/plugpass-production/performance-verify.log`·`.exit`, 별도 worktree `/private/tmp/plugpass-performance`의 runtime/MySQL 보고서. 운영 미검증 효과를 로컬 건수 개선과 구분한다.

### PR58 원격 잠금 테스트 순서 보정

- CI 38372016598에서 연결 종료 직후 재수집이 `SKIPPED`를 반환했다. TCP 연결 종료가 서버의 잠금 해제 완료를 보장하는 동기화 지점은 아니므로, 테스트 소유 연결에서 `RELEASE_LOCK` 응답 1을 확인한 뒤 재수집한다. 운영 잠금 구현도 명시적 해제를 사용한다.
- 수정 후 `python3 scripts/verify-mysql.py --tests-only`: MySQL 8개 통과, 종료 0. 성능 수정의 전체 검증 결과는 위 기록을 유지한다.

## 기능별 전달과 운영 실행 (2026-10-13)

| 기능 | PR | 확인한 결과 |
| --- | --- | --- |
| 단일 수집 CLI·DB 잠금 | [#57](https://github.com/seungmin-park/PlugPass/pull/57) | 필수 CI·머지·운영 배포 성공 |
| 전국 페이지 저장 병목 | [#58](https://github.com/seungmin-park/PlugPass/pull/58) | 필수 CI·머지·배포 후 전체527178건 수집 성공 |
| 상태 변경분·누락 복구 | [#59](https://github.com/seungmin-park/PlugPass/pull/59) | head e143766 CI38377444905 성공, native auto-merge dab999e, main CI38378536599·CD38379199727 성공 |
| 수집 상태 API·화면 | [#60](https://github.com/seungmin-park/PlugPass/pull/60) | head12449e7 CI38379069778 성공, native auto-merge13e9bee, main CI38379835959·CD38380315850 성공 |
| 운영 타이머·백업 | [운영 #4](https://github.com/seungmin-park/gcp-deployment/pull/4) | 대상13/최종 전체167 테스트 성공, 운영 systemd 구문 검증·타이머 활성화. CI38381542725/38381543506 성공, 후속 continue에 따라 일반 squash 머지a13aead |

### 실제 최초 수집과 보존

- 초기 실행은 페이지별 쿼리가 반복 flush를 유발해 첫 페이지를 완료하지 못했으며 SIGTERM으로 중단했다. 성공으로 기록하지 않았다. 이 중단 이력(run1)은 RUNNING으로 남아 있으나 실제 실행 프로세스는 없다. 화면은 이 과거 행을 현재 수집 중 상태로 표시하지 않고 가장 최근 실행과 성공 시각을 사용한다. 강제 종료 이력 자동 정리는 후속 보완 대상이다.
- 성능 수정 JAR release `plugpass-3ec5c217cf0e`/source5e8baf9의 run2는09:21:15Z 시작→09:31:25Z 완료,527178건·53요청·재시도0·종료0이다. 운영 DB의 충전소101082개/충전기527178개를 확인했다.
- 동일 cmux 브라우저에서 공개 서비스의 예시 좌표 검색→실제20개 충전소→첫 상세·길찾기 선택을 실제 클릭했다. 브라우저 오류0. 이 과정은 실제 내비 앱 설치·실행 검증으로 확대하지 않는다.
- V2는 PR59 성공 CI JAR sha256 `5a0831fd378ea1cd635126f70e586bc5bf7b029a5a755311759abb06f1597509`로 사전 migration·Hibernate 검증을 수행했다. migration 계정0·기존527178건 유지·공개 health UP 확인 후 머지했다.

### 백업과 주기

- 운영 백업 `/srv/deploy/backups/plugpass/20261013T093400Z/plugpass.sql.gz`를 네트워크 없는 별도 MySQL8.4.11에 복원했다. station101082/charger527178/sync_run2/flyway_schema_history1 모두 일치한다. 운영 DB를 복원 대상으로 쓰지 않았다.
- SHA-256 `247def187798b397f5f3041ed6369bf516abbcbafd9ca61ddd2e30afd3ffb33e`. 같은 검증본을 로컬 `~/.local/share/plugpass/backups/20261013T093400Z/`에도 보관했고 해시가 같다. 이 일회 복사와 자동 원격 보관을 구분한다.
- systemd 타이머: 수집5분마다, 백업02:30 Asia/Seoul. 기존 웹 슬롯은 수집 비활성화를 유지한다. 백업 실패는300초 후 재시도하며 로컬 검증 백업7일 보관이다. 기존 원격 버킷/설정이 없어 remoteCopied=false다.
- 수집 데이터/이력 자동 삭제는 없다. 공급자 응답 누락을 삭제로 해석하지 않고 폐쇄 원본 필드를 유지한다. 요청 실패 시 기존 페이지 commit과 이전 성공 시각을 보존한다.

### 검증 범위

최종 UI 소스 트리 `d51aea1716b1e8de66ee0ad0b5eea4120d1cdfa4`는 로컬 최종 검증과 main13e9bee에서 동일하다. Java451·프런트354·Python32·개발 브라우저31·JAR4·MySQL8·재시작 JAR7건 모두 실패/오류/skip0. 별도 상태 수집 PR은 Java447·프런트350·개발30으로 분리 검증했다. 각 PR의 [상태 수집](status-sync-verification.md), [화면](collection-status-verification.md) 기록을 함께 따른다. 문서 후속 갱신 자체는 새 애플리케이션 실행으로 표현하지 않는다.

### 자동 복구·예약 상태 갱신·운영 화면

- run3 CATALOG: 09:53:04Z→10:02:07Z, 전체527178건·53요청·재시도0·종료0. 마지막 성공 공백을 감지한 auto 모드의 실제 전체 복구이며 기존 충전기를 중복 생성하지 않았다.
- 10:05 타이머의 run4 STATUS: 10:05:44Z→10:06:17Z, 상태 변경16687건 성공. systemd Result=success/ExecMainStatus=0, DB 전체527178건 유지. 다음 타이머10:10Z, 백업17:30Z(다음 날02:30 KST) 예약을 확인했다.
- PR60 배포 release `plugpass-51e6772446b0`/green. 기존 SPA는 hash 이동만으로 새 번들을 읽지 않아 full reload 후 검증했다. 같은 cmux browser surface1000050013의375×812 화면에서 새로고침을 실제 클릭하고 수집 정상·성공 시각2026-10-13 19:06:17 KST·충전소18개를 확인했다. browser errors0/console entries0. 관측 시각이 없는 충전기의 최신성 확인 불가 안내는 유지된다.
- 운영 PR4 CI38379596532는 외부 DEMP main의 fixture 테이블 수 변경(15→17)으로 실패했다. 기존 전환 assertion을 유지하고 검증 기록의 DEMP 소스7981f365dd086745b76993f08b108d46635a044b로 checkout을 고정했다. 고정 계약 Red1→Green2, 전체167건 통과. 최종 head6574de3의 CI38381542725/38381543506 모두 성공했다. 실제 DEMP 서비스는 변경하지 않았다.

### 남은 운영 한계

자동 원격 백업·외부 알림 수신자는 아직 설정하지 않았다. 현 상태는 systemd 실패/재시도·journal 로그와 API/화면 지연 안내까지다. 백업 타이머 설치와 실제 수동 백업/복원은 확인했지만 다음02:30 예약 실행은 아직 관측 전이다. 성공 검증본은7일 보관하며 실패한 부분 백업의 자동 정리는 없다. 중단 run1의 RUNNING 이력 자동 정리와 저장 이력 보존 기간은 후속 정책이 필요하다. 운영 저장소의 보호 API403과 native auto-merge 비활성화는 유지된다. PR4는 후속 사용자 지시에 따라 아래와 같이 일반 머지했다.

### 운영 저장소 머지 제한 설명과 후속 전달

사용자가 기존 머지와 차이를 질문해 재조회했다. gcp-deployment는 private/allow_auto_merge=false이며 protection/rulesets 조회는 요금제403이지만 일반 머지 자체는 가능했다. 과거 PR1/2도 실제 머지 이력이 있다. PlugPass 저장소의 보호된 자동 머지와 다른 조건이다. 이를 설명한 뒤 사용자 continue에 따라 이 PR의 일반 머지를 진행했다. 일반 전달 규칙을 저장소 전체에서 완화한 것으로 해석하지 않는다.

- PR4 head6574de314a7cd5a5424099ecb2e864053cf41770, base d8419fe. CI38381542725/38381543506의 verify/runtime 모두 SUCCESS, mergeStateStatus CLEAN을 확인했다.
- `gh pr merge 4 --repo seungmin-park/gcp-deployment --squash --match-head-commit 6574de314a7cd5a5424099ecb2e864053cf41770` 후2026-10-13T10:40:02Z에 MERGED, main a13aeaddd83d3830ac68c8f769e9a22681abad08을 확인했다. native auto-merge나 보호 규칙 강제를 수행했다고 보고하지 않는다.
- 실제 JVM 전환 CI는 요청1800건·오류/타임아웃0·쓰기36건 재조회 일치·복구 시나리오4개 통과다. 격리 DEMP fixture이며 운영 DEMP 전환 결과로 확대하지 않는다.
- 배포 중10:25 수집은 stable 검사에서 시작 전에 거부됐다. 별도 수동 실행 없이10:30 타이머가 새 배포 JAR로 run8 STATUS를 시작해10:31:08Z에13534건 SUCCESS·종료0을 확인했고 전체527178건은 유지됐다.
- 머지 후 main CI의 최종 결과는 [운영 PR4](https://github.com/seungmin-park/gcp-deployment/pull/4)의 전달 기록에서 확인한다.
