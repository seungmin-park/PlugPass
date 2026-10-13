# T38 위치 복구와 운영 예시 제거

2026-10-13. 브랜치 fix/ios-location-recovery-work, 작업 경로 /tmp/plugpass-fix-ios-location-recovery.

## 문제와 책임

운영 화면에 검증용 합성 위치가 노출됐고 위치 실패가 모두 예시 선택으로 이어졌다. composable은 위치 API의 환경/오류와 요청 수명을, View는 재시도/주소 입력 포커스를 소유한다. 원래 요청 순번과 완료 플래그가 이전 callback을 거부하므로 이를 유지했다. 권한 자체를 앱이 강제로 변경하지 않는다.

```text
현재 위치 클릭 → 보안 연결/API 확인 → 브라우저 요청 → 성공 좌표
                                        └ 실패 → 원인 안내 → 재시도 / 주소 입력
새 요청 / 이탈 → 이전 요청 결과 무시
```

## Red / Green / Refactor

- 기존 위치14건 통과 후 비보안 접속·권한 설정 안내/주소 대안·미지원 대안·운영 예시 미노출을 추가했다. 첫 Red4건 중 비보안 테스트는 미구현 때문에 Promise가 끝나지 않아 테스트 timeout이 났다. 테스트를 즉시 호출 여부/안내 assertion으로 고쳐 위치3건의 assertion 실패를 재확인했다.
- Red 명령: `npm --prefix frontend run test:unit -- src/shared/location/useCurrentLocation.spec.ts src/features/search/views/StationSearchView.spec.ts`. 로그 /tmp/plugpass-t38-red.log, 보정 후 /tmp/plugpass-t38-red-location.log.
- Green: 비보안 접속을 HTTPS 안내로 구분, 모든 실패에 주소 대안 제공, 기기/사이트 권한 안내, 재시도/주소 포커스 버튼, 예시 함수/버튼/문구 제거.
- 테스트의 합성 위치는 제품 버튼이 아니라 위치 API 경계/Playwright context geolocation에서 제어한다. 실제 수집/DB/API journey assertion은 유지했다.
- Refactor: 작은 기존 composable의 요청 순번/완료 정책과 View 책임을 유지했다. 새 인터페이스/추측성 권한 캐시를 만들지 않았다. 이름·호출부·공개 URL/API 계약을 검토했고 변경 없음.

## 검증과 수정 과정

- 프런트356건 통과. 첫 지도 실패는 테스트 navigator 대체가 SDK의 userAgent/platform을 지운 fixture 문제였으며 두 정보를 유지해 복구했다. 제품 지도 동작을 완화하지 않았다.
- 첫 공용 verify는 개발 E2E30통과/2실패, 종료1. 주소 getByLabel이 region과 input을 함께 잡는 strict 오류, 기존 키보드 테스트의 예시 버튼 가정이었다. searchbox 역할과 현재 위치 권한 경계로 수정했다.
- 독립 read-only 리뷰: 차단 결함 없음. 비보안 상태/좌표 및 오류2/3의 주소 대안 assertion 보강 의견을 반영했다.
- 두 번째 실행은 개발 E2E31통과/1실패였다. 키보드 현재 위치 요청 중 status가 로딩/선택 안내 두 개라 생긴 strict selector 오류를 정확한 안내로 좁혀 수정했다.
- 최종 `bash scripts/verify.sh` 종료0: Java451·프런트356·개발E2E32·JAR4·MySQL8·MySQL 재시작JAR브라우저7, 실패/오류/skip0. type-check/lint/build 통과. 실제 MySQL8.4.11 재시작 후 데이터/readiness 보존과 설정 누락 거부 확인.
- 로그 `/tmp/plugpass-t38-verify.log`, JUnit `build/test-results/{test,mysqlTest}`, runtime `build/verification/runtime.json`, 브라우저 HTML/trace `frontend/playwright-report`·`frontend/test-results`. 검증 pane은 유지했다.
- PR/원격CI/실제 머지는 전달 후 추가한다.

## cmux와 실기기 한계

호출 workspace:1000050003 / surface:1000050003, 기존 우측 pane:1000050009의 검증 터미널 surface:1000050018을 만들었다. browser surface:1000050013에서 로컬 개발 화면의 예시 버튼 미노출, 현재 위치 클릭→실패 안내→주소로 검색 클릭→실제 activeElement=place-query를 확인했다. 별도 headed Playwright는 위치/네트워크 fixture를 지원하며 cmux WKWebView의 실제 OS 위치 성공은 그 fixture와 다르다.

운영 HTTPS HTML200, 응답에 위치 차단 Permissions-Policy 없음을 확인했다. 이것만으로 기기/사이트 권한이나 iPhone 위치 서비스 성공을 증명하지 않는다. iOS 실기기 설정 변경→복귀→재시도는 미검증이며 해결 완료로 표시하지 않는다. 외부 위치 실패는 DB 상태를 변경하지 않으므로 DB 장애/트랜잭션 변경 테스트는 새로 추가하지 않았다. 기존 MySQL 검증은 공용 verify로 유지한다.

## 공식 근거

[W3C Geolocation](https://www.w3.org/TR/geolocation/)의 secure context·권한·오류·getCurrentPosition을 2026-10-13 재확인했다. 앱 전체10초 제한과 이전 callback 무시는 프로젝트 정책이다. Vue3.5.43·TypeScript6.0.3·Playwright1.63.0을 기존 lockfile로 유지했다.
