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

본 실행의 결과와 원본 위치는 실제180초 실험 후 기록한다. 화면 표시 한계: 현재 cmux 호출 env·live socket 없음. 실제 loopback HTTP·터미널 실험이며 화면 E2E는 미확인이다. 목표 실패에 대한 개선 판단은 T16에서 다룬다.
