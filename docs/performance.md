# 조회 성능 기준선 — T15

## 재현 조건

```bash
python3 scripts/performance/load.py --output /tmp/plugpass-performance-새실행명
```

JDK25·Python3.9+·프로젝트 의존성 캐시가 필요하다. 매번 새 출력 폴더를 사용한다. 도구는 실제 소스의 testClasses를 준비하고 test classpath의 별도 loopback 서버를 띄운다. 사용자 서버나 기존 프로세스를 종료하지 않는다. `-Xms512m -Xmx2g`, H2 메모리, 단일 JVM, Boot4.1.1, Hibernate7.4.5.Final, Statistics 켜짐. seed·측정 endpoint는 test 소스에만 있어 운영 JAR에 포함되지 않는다.

생성 데이터는 grid-v1·seed0으로 고정한다. 난수 없이 index로 생성하며 index0..9999가 100×100 격자에 놓인다. 위도37.5+(index/100 정수)*0.001, 경도127+(index%100)*0.001. 충전소당5개 중4개 DC_COMBO,1개 AC_SLOW. index%4별 RECENT_AVAILABLE·UNVERIFIED_AVAILABLE·STALE_AVAILABLE·RECENT_OCCUPIED를 분배한다. sourceObservedAt은 합성이고 Clock은2026-10-05T00:00:00Z로 고정한다. 도메인 생성자/Builder와 실제 Repository saveAll로 준비·commit하며 seed 시간은 측정에서 제외한다. 수집 성능·실제 공급자 데이터·데이터 재시작 보존을 측정하지 않는다.

| 요청 | 예정 분포 | 입력 |
| --- | --- | --- |
| 주변 검색 edge | 25% | 37.5,127 ·1000m·DC_COMBO·limit20 |
| 주변 검색 center | 25% | 37.55,127.05 ·같은 조건 |
| 상세 | 25% | 첫 충전소 ID1, 충전기5개 |
| 추천 | 25% | edge 조건, ID1 제외, 그룹별20 |

20개 worker를 barrier로 시작하며 응답 뒤 다음 요청을 보낸다. 각 worker는 위4개 경로를 순환하고 시작 offset을 분산한다. **closed-loop 동시 사용자 실험**이며 고정 RPS/open-loop 수용 한도는 아니다. 실제 비율은 결과의 경로별 요청 수로 확인한다.

30초 준비 부하 후 별도180초 측정. warmup은 p50/p95에 포함하지 않는다. 측정 종료 전 시작한 요청은 응답까지 기다려 drain 시간을 따로 기록한다. HTTP 요청→응답 body 완료 지연을 측정하며 JSON 정확성 검사는 요청 사이의 부하 속도에 포함된다. Python client와 서버는 같은 머신이므로 클라이언트·다른 프로세스·GC 경쟁의 영향이 있다. 네트워크 timeout60초·연결 재생성·unexpected error를 기록한다. p50/p95는 nearest-rank이며 전체/경로별, 성공과 실패 모든 시도를 포함한다. 목표는 전체 p95≤300ms·예상하지 않은 오류율<1%. 목표 미달도 측정 결과로 보존하며, 예상하지 않은 HTTP/응답 실패가 있으면 종료 실패한다.

T14 정확성의 검색→상세→ID 제외 추천·최신성 분리 assertion을 생성 데이터에서도 유지한다. 부하 전 전체 반환 원소의 ID·거리·건수·상태/이유를 계산된 기대값과 비교하고, 모든 부하 응답을 검증한 전체 응답과 비교한다. 부하 후 같은 assertion과 전후 응답 동일·10,000/50,000 건수를 확인한다. SQL은 부하 전에 단독 HTTP 요청 전후 Statistics 차이로 세며 JDBC metadata count는 그 통계에 포함되지 않는다.

출력: `result.json`(SHA·dirty·환경·조건·p50/p95·오류율·SQL/엔티티 수·전후 정확성), `samples.json`(시도별 경로·worker·시작/지연·실패), `correctness-before/after.json`, `server.log`. 원본 샘플 sha256도 기록한다. `--users 2 --warmup 0 --seconds 1`은 실행 도구 smoke이며4주차 성능 기준선을 대신하지 않는다.

## 측정 결과

2026-10-05, Apple M2·8 logical CPU·16GiB·macOS27 arm64·Zulu Java25.0.4.1·Python3.9.6. 측정 소스 `a8522694986039fc45c1b2f475434b30f6772553`, dirty=false. 실행/샘플 저장/전후 assertion/서버 종료 모두 완료, 도구 종료0·직접 시작한 서버의 종료143(SIGTERM).

| 경로 | 요청 수 | p50 ms | p95 ms | SQL/요청 | 엔티티 로딩/요청 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 전체 | 4,591 | 768.01 | 1,425.41 | 아래 경로별 | 아래 경로별 |
| 검색 edge | 1,150 | 862.53 | 1,555.97 | 2 | 60,001 |
| 검색 center | 1,147 | 887.10 | 1,547.75 | 2 | 60,001 |
| 상세 | 1,145 | 354.88 | 736.61 | 2 | 6 |
| 제외 추천 | 1,149 | 817.52 | 1,420.78 | 2 | 60,001 |

준비 요청990건·오류0. 측정 요청4,591건·unexpected error0(0%)·응답 불일치0. 20개 worker 모두 참여했고 실제 경로 비율은 각24.94~25.05%. 설정180초·drain 포함180.788초, 완료 요청/drain 포함 시간 약25.39요청/초. 전후 정확성과10,000/50,000 건수 유지. 전체 p95≤300ms **미달**, 오류율<1% **달성**. 성능 목표 미달을 숨기거나 목표를 바꾸지 않았다. 측정 도구 종료0은 실험 완료를 뜻하며 latency target true를 뜻하지 않는다.

원본 [result.json](evidence/week4-performance/result.json)·[샘플 gzip](evidence/week4-performance/samples.json.gz)·[실행 조건](evidence/week4-performance/context.json)·[서버 로그](evidence/week4-performance/server.log)·[전](evidence/week4-performance/correctness-before.json.gz)/[후](evidence/week4-performance/correctness-after.json.gz) 응답. 원래 비압축 출력은 `/tmp/plugpass-week4-baseline`이다. 샘플을 압축 해제한 SHA256은 result.json의 samples_sha256과 실제로 일치함을 확인했다. 측정 이후 문서·증거만 추가하므로 source SHA의 측정 코드와 최종 PR의 관계를 확인할 수 있다.

```mermaid
flowchart LR
  Request[반경1000m·limit20] --> Read[모든 충전기50000·충전소10000 읽기]
  Read --> Filter[Java에서 호환·거리 필터]
  Filter --> Sort[정렬·후보 판단]
  Sort --> Limit[최종20개 응답]
```

SQL2개라서 N+1은 이 경로에서 관측하지 않았다. 대신 limit/반경/커넥터가 전체 fetch 후 적용되어 검색·추천이 매 요청마다60,001엔티티(성공 이력1 포함)를 로딩한다. 소스와 실제 통계가 일치하므로 T16의 첫 개선 후보는 DB에서 조회 후보를 줄이는 것이다. 상세도 혼합 부하에서 지연이 커졌으나 본 실험은 CPU/GC/connection 대기 시간을 분해하지 않아 단일 원인을 단정하지 않는다. 캐시·DB 교체를 추가하지 않았으며 개선 성공은 동일 조건의 전후 실험으로 별도 판단해야 한다. 화면 표시 한계: 현재 cmux 호출 env·live socket 없음. 실제 loopback HTTP·터미널 실험이며 화면 E2E는 미확인이다. 목표 실패에 대한 개선 판단은 T16에서 다룬다.

## T16 동일 조건 재측정 — 2026-10-05

측정 commit `4cf49493352817302ed81996c73ba967b1e26b1a`, 시작 시 dirty=false. 기존과 같은 머신·JVM/데이터/시간/동시성 조건이다. 기준선의 전체 응답 JSON과 새 측정 전후 응답 JSON이 동일함을 별도로 확인했다.

| 경로 | 이전 p95 ms | 개선 p95 ms | 개선 SQL/요청 | 개선 엔티티/요청 |
| --- | ---: | ---: | ---: | ---: |
| 전체 | 1425.41 | 75.04 | 경로별 | 경로별 |
| detail | 736.61 | 37.20 | 2 | 6 |
| recommendation | 1420.78 | 61.62 | 2 | 541 |
| search_center | 1547.75 | 109.33 | 2 | 1956 |
| search_edge | 1555.97 | 61.30 | 2 | 541 |

측정 176,051건·준비 52,188건, 오류/응답 불일치0. 전체p50 11.11ms·p95 75.04ms, 전체p95≤300ms/오류율<1% 달성. 종료0, 직접 시작한 서버 종료143. 전후10,000/50,000건수·검색/상세/추천 정확성 유지. closed-loop 실험의 같은 머신 관측이며 운영 환경·고정RPS 수용량 보장은 아니다.

[결과](evidence/t16-performance/result.json)·[샘플](evidence/t16-performance/samples.json.gz)·[서버 로그](evidence/t16-performance/server.log)·[전](evidence/t16-performance/correctness-before.json.gz)/[후](evidence/t16-performance/correctness-after.json.gz). 원본 `/tmp/plugpass-t16-measurement`, 샘플SHA256 검증 일치. [T16 Red/Green·책임 기록](t16-verification.md).
