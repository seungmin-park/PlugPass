# T40 충전소에서 바로 길찾기

2026-10-13. `feat/station-quick-directions`, `/tmp/plugpass-feat-station-quick-directions`. T39가 병합된 `main`9a0126f에서 시작했다.

## 사용자 흐름과 책임

```text
검색 카드 길찾기 → 처음: 선택 충전소의 하단 패널 → 카카오맵/네이버 지도 선택
                 이후: 기억한 앱의 해당 충전소 목적지로 바로 연결
지도 마커 선택 → 같은 충전소의 하단 패널 → 길찾기 또는 상세
상세 길찾기 → 기억한 앱 바로 연결 / 지도 앱 변경 / 웹 지도 대안
```

검색 View는 카드와 마커가 공유하는 선택 ID를 소유한다. `StationActionSheet`는 하단 패널과 초점 복귀, `StationDirections`는 앱 선택, 선호 Store는 현재 앱과 저장소 복구, `navigationLinks`는 OS별 목적지 URL을 소유한다. 앱 선택 뒤 카드 버튼이 직접 링크로 바뀌어도 같은 ID를 사용해 닫기 초점을 새 링크에 복귀시킨다. 외부 앱 설치 여부는 브라우저에서 알 수 없으므로 앱 설치 안내와 웹 대안을 유지한다.

## Red → Green → Refactor

- 카드의 직접 길찾기와 마커 패널 미구현을 실패 assertion으로 확인했다. `/tmp/plugpass-t40-red.log`.
- 저장 선호 읽기·쓰기와 예외 복구가 없는 상태를 실패 assertion으로 확인했다. `/tmp/plugpass-t40-preference-red.log`.
- Escape 처리, 다른 마커 선택 후 목적지 불일치, 패널 내 이용 조건 요약 누락을 각각 재현했다. `/tmp/plugpass-t40-escape-red.log`, `/tmp/plugpass-t40-target-red.log`, `/tmp/plugpass-t40-summary-red.log`.
- 처음 앱 선택으로 버튼이 링크로 교체된 뒤 패널 닫기 초점이 본문으로 빠지는 회귀를 실제 실패로 확인했다. `npm run test:unit -- src/features/search/views/StationMapSelection.spec.ts`에서 11건 중 1건 실패; 기대 링크와 실제 `body`를 비교했다.
- 최소 수정으로 선택 ID를 통일하고, 앱 선호 저장·직접 링크·패널·이용 조건 요약과 초점 복귀를 연결했다. 초점 회귀 대상 테스트는 11건 통과했다. 별도 길찾기 정책을 패널에 복제하지 않았다.

정상 경로는 첫 선택·반복 사용·앱 변경·마커/카드 목적지 일치·닫기/키보드를 검사한다. 경계는 잘못된 좌표·빈 이용 조건·잘못된 저장 값·저장소 예외다. 외부 앱 실행 성공 여부와 실제 iOS 설치 앱 전환은 로컬 브라우저 자동화로 검증할 수 없다. 공개 검색 URL/API 계약은 바꾸지 않았다.

## 실행 결과와 한계

공용 `bash scripts/verify.sh`를 현재 cmux 작업 공간의 보조 터미널 `surface:1000050020`에서 headed 브라우저 러너와 함께 실행했다. 종료 코드 0, Java 451건·프런트 단위 366건·개발 브라우저 37건·배포 JAR 4건·MySQL 8건·재시작 후 브라우저 7건 통과, 실패/오류/skip 0건이다. 타입 검사·lint·build도 통과했다. 원본 로그는 `/tmp/plugpass-t40-verify.log`, 보고서는 `build/verification`, `build/test-results`, `frontend/test-results`에 있다. 독립 읽기 전용 리뷰에서 남은 차단 코드 결함은 없었다. `docs/feature-map.md`의 검증 기록 링크를 이 문서로 연결했다. cmux 브라우저 surface는 숨김 상태여서 그 화면을 사용자에게 보이는 검증으로 보고하지 않는다. 실제 headed Playwright 실행과 로그는 보조 터미널에 표시했다. 실제 외부 지도 앱 연결과 iOS 권한 복구는 실기기 확인이 필요하다.
