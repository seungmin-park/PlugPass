# 원격 백업·외부 알림 후속 (2026-10-15)

사용자가 운영 흐름·실기기 사용에서 아직 문제를 발견하지 못했다고 알리고 T37 운영 후속을 요청했다. 이 기록은 사용자 관측을 에이전트 실기기 테스트로 바꾸지 않는다.

구현 소유자는 `seungmin-park/gcp-deployment`다. PlugPass 애플리케이션·DB 스키마를 변경하지 않았다.

```text
매일 02:30 KST → 덤프 → 별도 MySQL 복원 → 비공개 GCS → 재다운로드 해시 확인
5분 상태 보고 → 백업 26시간·수집 15분 기준 → Cloud Monitoring → 지정 Slack 채널
서버 밖 HTTPS 검사 → /healthz·인증서·UP 응답 → 장애·복구 알림
```

## 확인한 결과

- 기존 예약 백업은 2026-10-15 02:30 KST에 시작하고 수집 잠금 때문에 13회 재시도 후 03:36 KST에 성공했다. 이전 기록의 '다음 예약 실행 미관측'을 실제 journal로 해소했다.
- 전용 버킷 `plugpass-backups-project-687f2332`: 서울 리전, 공개 접근 차단, uniform access, 7일 lifecycle, soft delete 0. VM에는 이 버킷의 객체 생성·조회 역할을 추가했다.
- 첫 원격 업로드는 systemd NoNewPrivileges와 Snap launcher 충돌로 실패했다. 보호 설정을 유지하고 설치된 SDK를 직접 사용하도록 PATH를 지정했다. 자동 재시도에서 17:59:42 KST에 원격 성공, 추가 실제 실행도 18:01:01 KST에 성공했다.
- 성공본 `plugpass/20261015T085947Z`: station101115 / charger527554 / sync_run562 / flyway_schema_history2. SHA-256 `c9e63ecd83027c5e1c3f373d947ae67e9411a5ab7f7785c1d240ad1375b23832`. 격리 복원·원격 재다운로드 바이트 일치 확인.
- Slack 대상은 사용자가 지정한 `T0CA9M6NLD6/C0C993L3667`(#새-채널). 실제 테스트 알림과 합성 장애 알림 도착을 확인했다.
- 외부 HTTPS 검사에 처음 내부 경로 `/actuator/health`를 지정해 404 알림이 발생했다. 실제 공개 `/healthz`로 수정했고 외부 5개 위치의 최신 성공 및 18:02:50 KST 복구 알림을 확인했다. 서비스 장애로 기록하지 않는다.
- 상태 보고는 최근 실제 원격 성공과 수집 상태를 각각 1로 Cloud Monitoring에 기록했다. 상태 보고가 끊기면 VM 밖의 15분 누락 조건이 감시한다.

## 범위와 전달

신규 설정 후 다음 야간 예약은 2026-10-16 02:30 KST다. 미래의 실행을 이미 관측했다고 하지 않는다. 기존 야간 타이머의 실제 실행과 신규 원격 설정의 자동 재시도·수동 실행은 확인했다. 장애 감지는 정책 지속 시간·플랫폼 평가 지연이 있으므로 즉시 전달을 보장하지 않는다. 원격 버킷 내 사후 삭제는 다음 백업·26시간 기준에서 감지하며 즉시 감시는 아니다.

GCS에서 SQL과 VERIFIED.json을 모두 내려받아 네트워크 없는 별도 MySQL에 복원하는 검증도 성공했다. 네 테이블 전체 건수와 위 SHA-256이 일치하고 운영 DB는 변경하지 않았다. 최종 운영 저장소 전체 unittest188건 성공, cmux 최종 종료0을 확인했다. 상세 구현·명령·실제 결과·복구 절차는 [운영 PR #5](https://github.com/seungmin-park/gcp-deployment/pull/5)와 해당 PR의 `docs/plugpass-remote-backup-alerts.md`가 소유한다.

최초 전달 당시 운영 PR #5는 보호 규칙 조회403·native auto-merge 비활성 상태이므로 PR까지만 전달했다. 이후 상태는 아래 재확인 결과를 따른다. 이미 확인한 운영 적용과 원격 CI/실제 머지는 별개다.

## 전달 상태 재확인 (2026-10-15 21:34 KST)

- GitHub API에서 [운영 PR #5](https://github.com/seungmin-park/gcp-deployment/pull/5)의 `merged=true`, 병합 시각 2026-10-15 18:51:39 KST, 병합 커밋 `fa645e3d576c0f140eb2e85fd25afba82a754f41`을 확인했다. 이번 확인에서 머지를 실행하지 않았으며 보호된 자동 머지였다고 해석하지 않는다.
- 검증 head `785be8dfcac27587841f54565c8d38248e090434`의 [CI run 38711304076](https://github.com/seungmin-park/gcp-deployment/actions/runs/38711304076)에서 `verify`·`runtime` 모두 `success`를 확인했다. 이 조회는 기존 CI 결과 확인이며 운영 복원 시험을 새로 실행한 것은 아니다.
- 다음 예약 2026-10-16 02:30 KST는 확인 시각 이후다. 새 설정의 야간 예약 성공은 아직 미관측으로 유지한다. 예약 이후 타이머 실행 로그 → 원격 성공 기록 → 해당 실행의 GCS 검증본을 연결해 확인해야 한다. 수동 실행 성공으로 예약 실행을 대체하지 않는다.
