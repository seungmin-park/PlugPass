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
