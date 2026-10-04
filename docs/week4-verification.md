# 4주차 검증 기록

2026-10-05. 승인 범위: tasks.md T14/T15. 기존 Java25·Boot4.1.1·JPA/H2·REST Docs 유지, 프런트엔드/TypeScript 적용 대상 없음. [실행 계획](week4-plan.md).

## T14

- 기반: b45ac0d, codex/week4-journey. `./gradlew test --tests '*ChargingJourneyTests' --console=plain`:3건. 실제 loopback 공급자 HTTP·XML·production 수집 commit·H2·사용자 HTTP. 상세 시나리오: [데모](demo.md).
- 신규 검사기 Red: `python3 -m unittest discover -s scripts -p 'test_verify_runtime.py' -v`. API가 없는 최초 실행은 AttributeError2건으로 실행 준비 오류였다. 빈 검사 함수로 callable 계약을 마련한 뒤 누락/빈 산출물에 RuntimeError가 발생하지 않는 assertion 실패2건을 실제 확인했다. [Red 출력](evidence/week4-t14-red.txt).
- Green: 필수7종 HTTP 증거 파일·JSON 목록 필드·정확한 건수를 검사한다. 기존 업무 로직은 변경하지 않았다. 종합 테스트의 STALE 코드 기대값 오류(SOURCE_OBSERVATION_TOO_OLD)를 기존 계약 MAX_AGE_EXCEEDED로 바로잡았다. 이 실패는 기능 Red가 아니다.
- Refactor/리뷰: 실제 HTTP 스레드가 읽는 테스트 시계·응답·관측 시각은 volatile로 전달한다. Service는 실제 production transaction, cleanup은 해당 클래스 소유의 자식→부모 삭제. Controller·HTTP DTO·저장 schema·라우트·직렬화 계약 변화 없음. 도메인 판단을 테스트 fake로 대체하지 않는다. 규모만 맞추는 객체 분리는 불필요했다.
- 성공/실패/경계: 중복·거리0/반경 밖·관측 시각 누락·601초 경과·503 후 저장/성공 시각 유지·다음 성공 회복. 인증/429/timeout/부분 실패·동시 실행은 기존 T05/T11/T12 필수 suite를 유지한다. 실공공 인증·현장 충전·재시작 보존은 별도 범위.
- 최종 `bash scripts/verify.sh`: Java356건·Python11건, 실패/오류/skip0. 실제 JAR health200·검색200·추천200·validation400·없는 상세404·기본 metrics404·env404·REST Docs 동일. 로그: `/tmp/plugpass-t14-verify-final.log`, `build/verification/server.log`, `runtime.json`, `charging-journey/*.json`, `build/test-results/test/`.
- cmux: 호출 env 없음·identify/ping/capabilities live socket 없음·escalated identify 동일. 표시 불가의 원인을 확인했고 터미널 HTTP로 검증했다. 화면 E2E와 실제 공급자 키 인증은 미확인이다.

[T14 PR#28](https://github.com/seungmin-park/PlugPass/pull/28) 필수 CI SUCCESS·2026-10-05 03:44 KST 실제 merge ed855d45. 검증 head2aec903의 evidence.py run/check 모두0·current. 원격 main의 검증 지침 통합 후 재검증했다. T15는 이 main에서 진행한다.

## T15 준비와 검증

- Summary Red: `python3 -m unittest discover -s scripts -p 'test_performance.py' -v` assertion3건 실패(0인 percentile, 빈 분포 허용, 요청 수0). [출력](evidence/week4-t15-summary-red.txt). 최소 구현 후3건 통과. warmup을 p95에 섞지 않고 모든 unexpected error를 포함한다.
- Dataset Red: `./gradlew test --tests '*PerformanceDatasetTests'` expected16/actual0 assertion1건 실패. 최초 IN_USE enum 이름 컴파일 오류는 OCCUPIED로 바로잡은 준비 오류이며 Red로 세지 않았다. 생성 저장을 구현해1건 통과했다.
- Refactor: 최초 데이터 준비의 JDBC insert를 도메인 Builder/실제 Repository saveAll로 교체해 생성 불변식과 현재 매핑을 따르게 했다. 로컬 수동 seed SQL 파일에 의존하지 않는다. 동일 테스트 통과와 전체 규모 smoke의 HTTP 정확성을 확인했다. Metadata는 test 전용 JDBC count·Statistics 읽기만 맡고 업무 Controller/Service는 유지한다.
- 이름·경계: PerformanceDataset은 생성/저장, PerformanceApplication은 test 서버 진입, load.py는 고정 요청/정확성/집계/프로세스 정리를 맡는다. SQL/HTTP/JSON/도메인 공개 계약과 production 코드 변경 없음. 테스트 전용 endpoint/package는 운영 JAR에서 제외한다. TypeScript/Vue 없음.
- 2users/0초 warmup/1초 smoke는 도구 동작 확인이며 기준선 결과가 아니다. 최종 전체/실측 결과는 아래에 기록한다.

- 전체 `bash scripts/verify.sh`: Java357·Python14 실패/오류/skip0, 필수 suite34·JAR HTTP/문서 동일·performance 클래스 미포함.
- 실측: `python3 scripts/performance/load.py --output /tmp/plugpass-week4-baseline` 종료0. measured SHA a852269·dirty=false. 10,000/50,000·20worker·warmup30초·측정180초, 요청4591·오류0·p50 768.01ms·p95 1425.41ms. latency 목표 미달/error 목표 달성. 부하 전후 실제 HTTP 전체 응답·건수 동일, 모든20worker 참여, 원본 샘플sha256 일치. [표·원본·원인 경계](performance.md).
- SQL은 모든 경로2개지만 검색/추천60001엔티티, 상세6개를 로딩. N+1이 아닌 전체 fetch 비용을 T16 후보로 기록한다. 이번 범위에서 업무 최적화나 목표 조정은 하지 않았다. 최초 smoke 수치를 baseline으로 채택하지 않았다.
- 시간 경과·장애/복구·API validation·역순·예산/동시성은 기존 T14/T05/T11/T12 필수 suite를 유지한다. 성능 서버의 시계 고정은 측정 반복성을 위한 합성 조건이며 실제 공공 API 최신성 보장을 하지 않는다.

## 전달·완료 판단

| 작업 | 검증한 PR head | 실제 main merge | 원격 필수 CI |
| --- | --- | --- | --- |
| T14 [PR#28](https://github.com/seungmin-park/PlugPass/pull/28) | 2aec903296c169052e2434a854567072f0f4f396 | ed855d45cd92d8f5be177979c6e7c6524f83726e | [SUCCESS](https://github.com/seungmin-park/PlugPass/actions/runs/37225455521/job/111504061207) |
| T15 [PR#29](https://github.com/seungmin-park/PlugPass/pull/29) | 079831549281646534cbfb0ff8185b7209dd7351 | a7793423c7e8f71e65f8728790845676f98951e7 | [SUCCESS](https://github.com/seungmin-park/PlugPass/actions/runs/37226353722/job/111506694737) |

T15 원격 원본 로그에서도 Python14·Java357·실패/오류/skip0·JAR HTTP/문서 일치를 확인했다(`/tmp/plugpass-t15-ci.log`). 최종 head0798315의 전역 evidence run/check는 모두0·current(`/tmp/plugpass-t15-proof`). 로컬 측정은 별도 dirty=false SHA a852269이며, 0798315의 측정 코드와 동일하고 이후 차이는 문서/원본뿐이다. 보호 strict=true·GitHub Actions app15368의 PlugPass verify·관리자 적용·승인0·강제push금지와 native auto/squash를 실제 조회한 뒤 자동 머지를 신청했다. 2026-10-05 03:59 KST T15 실제 머지 이후 체크한다.

4주차 8개 체크는 구현·실제 검증·main 반영 완료다. 조회 지연 목표300ms 미달·cmux 화면/공공키 인증 미확인·H2 재시작 비보존은 그대로 남긴다. 성능 개선은 T16, 장애 종합은 T17 후속 범위이며 앞당겨 완료 표시하지 않는다. 문서 참조 확인을 애플리케이션 실행으로 표현하지 않는다.
