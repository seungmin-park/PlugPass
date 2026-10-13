# T35 상태 갱신 기능 검증

## 범위와 책임

`feat/charger-status-sync`는 상태 API 수집과 자동 수집 선택만 전달한다. 수집 상태 HTTP/화면과 운영 타이머·백업은 별도 PR이다. agent-engineering과 기존 verify-plugpass를 사용한다.

```text
CLI auto → ScheduledSyncService → CollectionSchedule
  ├─ 전체 성공 없음 / 24시간 경과 / 성공 이후 10분 누락 → StationSyncService
  └─ 최근 성공 → StatusSyncService → 동일 DB 잠금 → 상태 API → StatusUpsertService → Charger
```

외부 HTTP와 페이지 저장 트랜잭션을 분리한다. 상태 저장은 기존 식별자만 조회하며 좌표·커넥터·운영 정보를 만들거나 삭제하지 않는다. 페이지당 일괄 조회 후 도메인 변경 메서드로 적용한다. 상태 실행 성공은 전체 데이터 준비를 설정하지 않는다. 기본 정보는 200페이지/300요청/30분 상한을 운영에서 선택하고 상태는 20페이지/40요청/120초·1회 재시도로 제한한다.

공급자의 최근10분 변경분은 고정 스냅샷이 아니다. 페이지 사이 전체 건수 변경을 허용하되, 페이지 불일치·페이지 내 중복·조기 빈 페이지·깨진 응답은 실패다. 같은 충전기의 이전 변경 시각은 적용하지 않는다. 기본 정보 응답이 이전 상태를 포함하면 운영 정보만 갱신하고 최신 상태를 보존한다. offset 없는 변경 시각을 관측 Instant로 변환하지 않아 기존 UNVERIFIED 판정은 유지한다. 이는 공급자 최신성 보장이 아닌 프로젝트의 역전 방지 정책이다.

## Red → Green

- 상태 HTTP 변환, 상태만 변경하는 도메인, 실제 commit 후 재조회, STATUS 이력 분리, 최초 준비 판정에 대해 누락 assertion 실패를 먼저 확인했다.
- `StatusUpsertTests`에서 20건 입력의 SELECT 20회 > 1회 실패 후 일괄 식별자 조회로 수정했다.
- `CollectionScheduleTests`: 최근 성공에서도 CATALOG를 선택하는 실패 후 24시간/600초 경계를 구현했다.
- `IngestionCommandTests`: status 모드가 CATALOG 이력을 남기는 실패 후 명시적 catalog/status/auto 선택을 연결했다. 자동 모드의 최초 전체→상태 전환도 실제 Repository로 검증했다.
- `ChargerStatusChangeTests`: 전체 정보가 최신 OCCUPIED를 AVAILABLE로 되돌리는 실패 후 상태 보존·운영 정보 갱신을 분리했다.
- 기존 `StationUpsertTests`의 “마지막 도착 상태 우선” 기대는 새 상태 변경 시각 비교 정책과 충돌했다. 이전 상태 유지 및 sourceObservedAt=null을 함께 검증하도록 계약을 갱신했다. assertion을 제거하거나 상태를 최신 관측으로 승격하지 않았다.
- 외부 SERVER 연속 실패 후 기존 데이터·성공 이력 보존과 다음 회차 복구, 없는 충전기 무시, 빈 변경분 성공, 중복 계약 거부를 검증했다.

## 이름·설계 검토

StatusSyncService는 페이지 실행·예산·완료 이력, StatusUpsertService는 저장 트랜잭션, ChargerStatusChange/Charger는 상태 시각 비교와 필드 보존을 소유한다. CollectionSchedule은 순수 시간 정책이다. RetryingPageFetcher는 기존 제한 재시도를 두 페이지 유형이 공유하도록 제네릭으로 바꾸었다. Controller·HTTP DTO·프런트는 이 PR에서 변경하지 않는다.

## 실행 근거

분리 전 통합 작업 트리의 백엔드451건·프런트354건·개발 브라우저31건·JAR4건·MySQL8건·재시작 JAR7건을 확인했다. 이 결과는 분리된 PR 자체의 통과 근거를 대신하지 않는다. 분리한 현재 소스의 전체 verify 및 원격 CI 결과를 아래에 추가한다.

V2 migration은 sync_run.kind를 기본 CATALOG로 추가하고 조회 인덱스를 만든다. 기존 행을 지우지 않는다. 운영 앱은 DML 계정이므로 머지/자동 배포 전에 임시 마이그레이션 권한으로 적용·기존 웹 호환성을 확인해야 한다. 운영 타이머는 후속 PR까지 활성화하지 않는다.

### 분리 소스 전체 검증

`/private/tmp/plugpass-status-sync`에서 현재 cmux 보조 pane으로 `bash scripts/verify.sh`를 실행했다. 종료0, Java447·프런트350·Python32·개발 브라우저30·배포 JAR4·MySQL8·MySQL 재시작 JAR7건, 실패/오류/skip0. 로그 `/tmp/plugpass-production/status-feature-verify.log`, JAR SHA-256 `cb34dc073dd9861afc02e58245b4f96235bae4d9a7132d9c967e183094286825`. UI 기능을 제외한 현재 소스에서 다시 실행한 결과다.

관련 선행 PR58 운영 수집은 2026-10-13T09:31:25Z에 전체527,178건·요청53회·재시도0·종료0으로 완료했다. 동일 cmux 브라우저에서 공개 서비스의 예시 좌표 검색→실제20개 충전소→첫 상세·길찾기 선택을 클릭했고 브라우저 오류0을 확인했다. 이는 PR58의 운영 데이터 준비 근거이며 이 PR의 상태 API 운영 실행은 배포 후 별도 확인한다.

## 최종 전달·운영 확인

PR59 head e143766의 CI38377444905 성공 → 자동 머지 dab999e → main CI38378536599·CD38379199727 성공. 사전 V2 migration 후 기존527178건을 유지했다. auto 전체 복구527178건과 실제5분 타이머 STATUS16687건 성공을 확인했다. 상세 근거와 한계는 [운영 실행 기록](production-data-verification.md)을 따른다.
