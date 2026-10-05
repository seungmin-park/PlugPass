# T21~T24 최종 검토와 판단 기록

검토 대상은 `d6ac70a..875c28c92d6e02a15d97aabdd423a43b8704cc02`다. 별도 맥락의 검토자가 코드·테스트·커밋된 실행 근거를 읽었다. 검토자는 파일을 수정하거나 서버·테스트를 실행하지 않았다. 실제 실행 결과는 구현자가 현재 코드로 확인한다.

검토자는 Critical/Important 없음, Minor 1건으로 보고했다. 요청 취소와 순번 방어, 후보·수집 메타의 독립 상태, View→Store→API 책임, 개발 모드와 운영 JAR의 별도 검증에는 병합 차단 사항을 찾지 않았다.

## 상세 링크 기본 동작: Important로 재분류하여 수정

`StationCard`와 `CandidateGroup`의 무조건 `@click.prevent`가 Ctrl/Cmd/Shift 클릭도 현재 화면 이동으로 바꿨다. 사용자가 충전소 여러 곳을 새 탭·창에서 비교하는 실제 기능을 막으므로 단순 표현 개선보다 높은 우선순위로 판단했다.

```text
상세 링크 클릭
  ├─ modifier 없는 주 버튼 → 기본 이동 취소 → openDetail → View의 현재 화면 이동
  └─ Ctrl/Cmd/Shift/Alt·다른 버튼 → 실제 href → 브라우저 기본 처리
```

두 컴포넌트는 props와 event 계약을 그대로 소유하며 라우터나 Store를 직접 호출하지 않는다. `@click.exact.left.prevent`의 순서가 중요하다. `prevent`가 앞에 있으면 뒤에서 클릭을 거부해도 기본 동작은 이미 취소된다. 공유 객체를 추가할 만큼 독립 상태나 복잡한 규칙이 없으므로 Vue의 기존 필터를 사용했다. href·검색 조건·JSON·API·DB 계약은 유지했다.

테스트를 먼저 작성하고 두 컴포넌트의 Ctrl/Cmd/Shift/Alt/가운데 버튼 10건에서 `defaultPrevented=true`라는 의도한 assertion 실패를 확인했다. 일반 클릭 등 기존 동작 5건은 통과했다. 수정 후 대상 15건이 통과했다. 테스트의 후속 DOM listener는 컴포넌트 처리 결과를 기록한 다음 테스트 환경의 이동만 취소한다. 실제 새 탭 생성 자체를 이 단위 테스트로 증명하지는 않는다.

- Red: `npm run test:unit -- src/features/search/components/StationCard.spec.ts src/features/recommendation/components/CandidateGroup.spec.ts` → 10실패·5통과·종료1.
- Green: 같은 명령 → 15통과·종료0.
- [Red 로그](evidence/t24/link-red.log.gz)·[Green 로그](evidence/t24/link-green.log.gz).

## 판단과 비용

아래 항목은 검토자가 판단 범위에서 제외한 동작을 포함한다. 미검증을 통과로 바꾸지 않았다.

| 판단 | 이유 | 판단이 틀렸을 때의 비용 |
| --- | --- | --- |
| 기존 tasks.md·frontend-plan을 실행 기준으로 재사용 | 중복 계획을 만들지 않고 순차 완료와 근거를 같은 체크리스트에 연결 | 문서 사이 불일치가 있으면 누락 위험이 있어 기능 지도·실행 결과를 함께 대조해야 함 |
| 상세 링크 결함을 Important로 재분류 | 새 탭 비교라는 일반 링크 사용을 막는 사용자 동작 결함 | 범위를 너무 넓게 잡았다면 작은 수정·회귀 테스트 비용이 추가됨 |
| 실공공 API 인증은 T01 미완료로 유지 | 인증키가 없고 합성 공급자 검증과 실공공 응답 검증은 다름 | 실제 공급자의 인증·응답 차이는 키를 받은 뒤 추가 검증해야 함 |
| 실제 기기의 위치 획득 성공은 미검증으로 유지 | 제어한 위치 경계와 권한 거부·예시 위치 복구를 검증함 | 기기·브라우저별 위치 성공 동작 차이가 남음 |
| 지도·주소 검색·경로 안내·운영 배포는 이번 완료 범위에서 제외 | 승인된 목록 중심 웹앱 계획의 후속 또는 제외 범위 | 해당 기능을 기대한 사용자에게는 추가 작업이 필요함 |
| H2 재시작 보존을 완료 조건으로 삼지 않음 | 메모리 DB 계약이며 소실·준비 상태 초기화·재수집을 검증함 | 재시작 후 기존 데이터 보존이 필요하면 저장 방식 변경이 필요함 |

이월한 Minor는 없다. 위 기능 결함은 한 번의 수정 과정에서 TDD로 해결하며 재검토 에이전트를 다시 호출하지 않는다. 현재 코드의 전체 검증과 필수 Linux CI를 확인한 뒤 T24를 체크한다.

## 수정 후 전체 실행 결과

현재 cmux workspace:5의 전용 surface:18에서 `bash scripts/verify.sh`를 실행해 종료0을 확인했다. Java361·프런트273/24파일·Python32·개발 E2E17·운영 JAR 브라우저2건, 실패/오류/skip0이다. 엄격한 타입/lint/build, 실제 웹앱 자산 HTTP와 REST Docs 일치, 테스트 클래스 부재, 별도 JVM3회 재시작의 소실·초기화·재수집을 확인했다. [전체 실행 로그](evidence/t24/review-verify.log.gz)·[실제 HTTP와 건수](evidence/t24/review-runtime.json)를 따른다.
