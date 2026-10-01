# PR·CI 전달 흐름 검증 기록

2026-10-01 로컬 PlugPass 작업 트리에서 실행했다.

- `JAVA_HOME=<로컬 JDK 25> bash scripts/verify.sh`: 종료 0.
- clean build: 성공. JUnit 테스트 4개, 실패·오류·skip 0.
- 실행 JAR: health 200/UP, env 404, API 문서 200 및 생성 HTML과 일치.
- JAR에서 테스트 전용 PersistenceProbe가 제외됨을 확인.
- 검증기의 거부 동작: 보고서 없음, 테스트 0개, failure, error, skipped 각각 RuntimeError. 정상 보고서는 허용.
- `bash -n scripts/verify.sh`: 종료 0.
- Ruby YAML parser로 workflow 구문과 PlugPass verify job 존재 확인. GitHub Actions에서의 실행 성공을 의미하지 않는다.
- cmux의 호출 workspace·surface 환경값이 없고 live socket도 연결되지 않았다. 터미널에서 실제 HTTP를 검사했으며 화면 E2E로 보고하지 않는다.

로컬 로그: `build/verification/server.log`, `build/verification/runtime.json`, `build/test-results/test/`. 빌드 로그는 `/private/tmp/plugpass-delivery/verify.log`에 남겼다. build·tmp는 커밋되지 않으며 다음 clean 실행 때 바뀔 수 있다.

원격 CI·브랜치 보호·자동 머지 결과는 별도로 실제 GitHub 상태를 확인해야 한다. 초기 조회에서 비공개 저장소의 Rulesets API가 요금제 제한으로 403을 반환했다. SSH 서명 인증 문제도 있었으며 이를 unsigned commit으로 우회하지 않았다.

## 원격 확인

- 초기 프로젝트를 서명 커밋하여 main에 push했다. SSH 서명은 TTY 연결 후 성공했다.
- [PR #1](https://github.com/seungmin-park/PlugPass/pull/1)을 생성했다.
- 커밋 `3affdf180f413af37640141ffb6728e138377ca6`의 [GitHub CI](https://github.com/seungmin-park/PlugPass/actions/runs/36874504695)가 성공했다. 원격 로그에서도 테스트 4개, 실패·오류·skip 0과 실행 JAR의 HTTP 검증 성공을 확인했다.
- 사용자의 공개 전환 후 main 보호 규칙을 적용했다. API에서 strict=true, GitHub Actions 앱 15368의 PlugPass verify 필수, enforce_admins=true, PR 승인 인원 0명, force push·삭제 금지를 확인했다.
- 저장소 allow_auto_merge=true, allow_squash_merge=true를 확인했다. 개별 PR의 실제 머지 완료 여부는 PR 이벤트와 main SHA로 확인한다.
