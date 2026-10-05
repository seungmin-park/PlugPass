# T01 실공공 API 계약 검증

2026-10-06. 작업 시작 소스: `8e13adb253efc78ee179389bdfc5ede1be1921d5`.
계약: [data-contract.md](data-contract.md), 작업: [tasks.md](../tasks.md).

## 관찰과 작업 범위

1Password의 `local` 환경에 있는 `DATA_API_KEY`를 임시 요청 프로세스에 전달했다. 키·키가 포함된 URL·연락처를 기록하지 않았다. 설치된 CLI는 변경하지 않고 공식 beta 배포본의 PGP 서명과 공식 공개키 지문을 확인했다.

처음에는 실제 HTTP403·공급자 코드30(등록되지 않은 서비스키), 실제 Java 클라이언트의 AUTHENTICATION을 관찰했다. 사용자가 홈페이지에서 동작을 확인한 키로 1Password 값을 갱신한 뒤 같은 요청이 HTTP200·결과00으로 성공했다. 원인이 등록 전파 지연이었다고 단정하지 않는다.

인천(`zcode=28`)·요청 크기10에서 총33,506건, 첫 페이지10건, 마지막3351페이지6건, 그다음3352페이지0건을 확인했다. 마지막과 빈 페이지의 응답 `numOfRows`는 각각6과0이다. 기존 클라이언트는 이를 요청 크기10과 다르다고 거부했다.

설계 판단: 요청 페이지 용량과 공급자가 보고한 건수를 구분한다. 실응답의 보고 건수와 기존 가이드 예제의 요청 용량 표기를 모두 지원하되, 둘 중 어느 것에도 맞지 않는 보고값·범위 초과·조기 빈 페이지는 계약 실패로 유지한다. 페이지 범위와 다음 페이지 계산에는 요청 용량을 쓴다. 이를 혼동하면 정상 마지막 페이지를 거부하거나 수집 종료를 잘못 판단한다.

```text
공급자 XML: numOfRows=6, items=6
  → XML parser: 구조·보고값·요청 범위 검사
  → StationPage: pageSize=요청 용량10, 실제 목록6
  → SyncProgress: 페이지 용량 일치·중복·전체 완료 검사
```

## Red → Green → Refactor

1. **Red:** `./gradlew test --tests '*PublicDataClientTests' --rerun-tasks`로 74건을 실행했다. 새 정상 마지막6건·빈0건 테스트가 `PublicDataException: CONTRACT`를 던져 `doesNotThrowAnyException` assertion 두 건이 실패했다. 기존 구현이 정상 표본을 거부한 것이며 컴파일·환경 실패가 아니다. [보존한 Red 로그](evidence/t01/pagination-red.log).
2. **Green:** XML parser에 요청 용량을 전달하고 범위 계산에 사용했다. 클라이언트는 보고값이 요청 용량 또는 실제 목록 건수인지 검사한 뒤 `StationPage`에 요청 용량을 넣는다. 첫 Green 실행에서 마지막 여섯 항목의 제한을 모두Y로 가정한 테스트 준비 실수를 발견했다. 원본 전체 목록의 `Y,Y,N,N,N,N`을 정확히 검증하도록 고쳤으며 이것을 기능 Red로 세지 않는다. 대상74건·전체Java371건이 실패/오류/skip0으로 통과했다.
3. **Refactor:** 외부 응답 DTO의 `pageSize`를 `reportedRowCount`로, 클라이언트의 `data`를 `providerResponse`로 바꿨다. 이름만 읽어도 공급자 보고값과 업무 페이지 용량의 책임 차이를 알 수 있게 했다. 새 인터페이스·위임 객체는 만들지 않았다. 대상74건·전체371건을 다시 통과한 뒤 공용 검증을 실행했다. 로그: `/private/tmp/plugpass-t01-pagination-green.log`, `/private/tmp/plugpass-t01-all-green.log`.

기존64건에 실표본·부정 입력10건을 추가했다. HTTP fixture 서버부터 실제 XML parser·상태 변환·`StationPage`까지 검증한다. 잘못된 보고값(-1/0/5/7/11), 요청 용량으로 계산했을 때 전체 범위를 넘는 응답, 조기 빈 응답은 모두 공개 CONTRACT 예외·메시지·cause 없음으로 거부한다. 기존 인증·timeout·429·5xx·깨진 XML·XXE·필수값·페이지 불일치 회귀도 유지했다. 저장 동작·시간 정책·동시성·재시도 정책은 바꾸지 않았으므로 별도 새 사례를 추가하지 않고 기존 공용 회귀를 실행했다.

## 실제 실행과 화면 확인

`bash scripts/verify.sh` 종료 코드0. Java371건·프런트290건(27파일)·Python32건, 개발모드 headed Chromium21건·배포 JAR headed Chromium3건이 통과했다. 실패/오류/skip0이며 타입 검사·lint·build, 실제 JAR의 HTTP·REST Docs·정적 자산, 별도 JVM3개의 메모리 데이터 소실·준비 상태 초기화·재수집도 통과했다. [로컬 요약](evidence/t01/local-summary.json)의 파일 해시와 `build/verification/runtime.json`을 연결한다. 전체 로그는 `/private/tmp/plugpass-t01-verify.log`, JUnit은 `build/test-results/test/`, 브라우저 결과는 `frontend/test-results/e2e.json`·`frontend/test-results/packaged.json`에 있다.

호출 정보를 확인한 cmux `workspace:4`의 오른쪽 보조 `pane:11`·터미널 `surface:12`에서 `/Users/seungmin/Desktop/repo/PlugPass`의 명령·서버·로그를 표시했다. 호출 터미널 `surface:4`는 사용하지 않았다. 같은 workspace 브라우저 `surface:13`에서 예시 위치 → 검색 → 첫 충전소 상세(ID1) → ID1을 제외한 대체 후보 → 다른 상세(ID2)를 실제 클릭했다. 두 상세의 관측 시각 미제공·최신성 확인 불가, 후보의 독립적인 수집 시각 안내를 확인했다. console은 Vite 연결 debug만 있고 브라우저 오류는 없었다. 브라우저를 서버 준비 전에 연 첫 시도는 연결 거부였으며 준비 완료 후 다시 이동해 전체 흐름을 확인했다. 보조 pane은 남겼다.

화면 흐름은 합성 공급자의 실제 HTTP 수집·H2 두 곳을 사용한다. Chromium 자동 테스트의 네트워크 mock을 쓰는 실패 사례와 cmux WKWebView의 직접 클릭을 구분한다. 수동 화면 확인으로 자동21건·3건의 assertion을 대체하거나 통과 수를 늘리지 않았다.

공용 검증 후 수정된 실제 `DefaultPublicDataClient`로 인천 첫/마지막/빈 페이지를 다시 호출해 요청 용량10·목록10/6/0·종료 여부를 확인했다. [실응답 근거](evidence/t01/live-contract.json)는 원본 응답 SHA-256, 정제 전후 표본 해시, Java 반환값, 초기 인증 실패를 보존한다. 여섯 XML fixture의 구문·구조·식별자·좌표·상태·건수와 키/연락처 미포함을 검사했다. 연락처·불필요한 필드는 allowlist로 제거하고 업무에 쓰는 값은 그대로 보존했다.

## 책임·계약·검증 한계

XML parser는 외부 구조·건수·요청 범위를, 클라이언트는 요청/응답 일치·변환을, `StationPage`는 업무 페이지 용량·종료 계산을 맡는다. `SyncProgress`의 페이지 용량 일치 계약을 유지한다. 내부 DTO 호출부를 모두 갱신했고 공개 HTTP JSON·DB·라우트·설정·의존성 버전은 변경하지 않았다. 단순 숫자 이름 변경을 위한 별도 객체 추출은 필요하지 않았다.

시간대가 없고 상태 변경 시각은 관측 시각이 아니므로 `sourceObservedAt=null`·UNVERIFIED를 유지한다. 계정별 호출 한도·현장 좌표 정확성·전 지역 실데이터 수집·실공공 API의 DB 저장/스케줄·실데이터 성능은 이번 세 페이지 검증의 범위 밖이다. 처음 인증 실패가 발생한 원인은 키 갱신 후 성공했다는 관찰 이상으로 확정하지 않는다.

## 전달

[PR #49](https://github.com/seungmin-park/PlugPass/pull/49)의 서명된 head `68a62d1d04afc65a8928ca17477178d0debb7b50`를 GitHub가 valid로 확인했다. 해당 head의 [필수 CI](https://github.com/seungmin-park/PlugPass/actions/runs/37348746456)는 SUCCESS이며 GitHub Actions 앱15368에서 발생한 `PlugPass verify`다. 내려받은 원본 JUnit·프런트·브라우저 보고서를 기존 guard로 재검사해 Java371·프런트290·개발21·배포3, 실패/오류/skip0을 확인했고 원본 로그의 Python32건도 대조했다. 검증한 소스·표본 해시는 로컬 요약과 같다. [CI/병합 근거](evidence/t01/ci-summary.json)를 보존한다.

strict 최신 base·관리자 적용·필수 검사·강제 push/삭제 금지 설정을 조회한 뒤 exact head match로 native squash auto-merge를 신청했다. 2026-10-05T17:34:43Z에 실제 main `a81e449807b48e0102bcdae77ed818fd0b15d8b0`로 병합된 것을 확인했으며 읽기 전용 `pr_status.py --goal merged`도 종료0·merged다. 로컬 main은 fast-forward로 반영했다. 이 근거를 연결한 문서 기록에서 T01 두 체크를 완료로 표시한다.
