# 1주차 실행 기록 — T03~T05

계획: [tasks.md](../tasks.md), 계약: [data-contract.md](data-contract.md). 순차 실행, 병렬 에이전트 없음.
T01 실응답은 사용자 선택(키 없음, 문서/fixture 진행)에 따라 미확인으로 유지한다.

## T03 — 상태 코드 정규화

- RED: `./gradlew test --tests '*ProviderStatusMapperTests' --console=plain`, 종료 1, 19건 중 6 assertion 실패·오류/skip 0.
  항상 UNKNOWN을 반환하는 최소 형태에서 1/2/3/4/5 매핑과 null 상태 거부가 실패했다.
- GREEN: 공식 코드표 2=AVAILABLE, 3=OCCUPIED, 1/4/5=UNAVAILABLE, 0/미지원/null=UNKNOWN.
  Charger에 normalized status와 nullable rawStatus를 별도로 보존한다. 원본의 공백·선행 0을 정규화해서 유효 코드로 추측하지 않는다.
- REFACTOR: 원본 보존 테스트에서 같은 mapper 호출로 기대값을 계산하던 불필요 assertion을 제거했다.
  매핑 결과는 독립 리터럴 기대값으로 별도 테스트한다. 책임이 작아 클래스 추출은 불필요했다.
- 기존 Charger(id) 생성은 UNKNOWN/null로 유지한다. 도메인은 ingestion 구현을 참조하지 않는다.
- 실패/경계: 정상 코드 전부, unknown·null·빈 값·공백·02·앞 공백, 원본 보존, null 업무 상태 거부.
  외부 timeout·DB·상태 변경 시각은 이 순수 매핑 단계에 적용 대상 없음.
- 이름/책임: ProviderStatusMapper는 외부 코드 해석, Charger는 상태/원본 소유. Service·HTTP·DB 계약 변경 없음.

## 실행 환경

`CMUX_WORKSPACE_ID`, `CMUX_SURFACE_ID` 없음. `cmux identify --json`은 샌드박스 안팎 모두 No live cmux socket found.
보이는 E2E를 수행한 것으로 보고하지 않는다. 공용 검증은 기반 HTTP와 JAR 확인이며 업무 흐름은 후속 범위다.
실행 로그는 `/tmp/plugpass-t03-{red,green,verify}.log`, 현재 XML은 `build/test-results/test/`에 있다.

T03 검증 결과: 대상 19건 통과, 최종 `bash scripts/verify.sh` 종료 0·전체 75건·실패/오류/skip 0,
health 200/UP, env 404, docs 200 및 패키징 문서 일치. 기존 Gradle deprecation 경고는 남아 있다.
