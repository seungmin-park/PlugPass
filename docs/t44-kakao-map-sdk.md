# T44a 카카오 지도 SDK 연결

## 목표와 경계

국내 지도 표기가 필요한 운영 화면에서 카카오 Web SDK를 사용한다. 주소·장소 후보는 기존 서버 REST 검색 경계를 유지하며, 이 PR은 지도 그리기와 선택 동기화만 바꾼다. 운영 주소 검색의 `PLACE_SEARCH_NOT_CONFIGURED`는 서버 REST 키 배포 작업으로 별도 해결한다.

```text
검색 화면 ── 지도 계약(선택·이동·영역) ── 카카오 SDK 어댑터
    │                                      └─ JavaScript 키·등록된 도메인
    └─ 충전소 API/목록 ── 서버 ── 공공데이터·카카오 REST
```

키가 없는 로컬 개발·기존 E2E는 기존 Leaflet 지도를 쓴다. 운영 CI에는 1Password 개발자 환경의 `KAKAO_JS_KEY`를 GitHub Actions secret으로 전달한다. `scripts/verify.sh`는 **배포 JAR 빌드 단계에만** `VITE_KAKAO_JS_KEY`를 주입한다. 이 값은 브라우저 SDK 키로 산출물에 노출되므로 카카오 개발자 콘솔의 JavaScript SDK 도메인 등록이 필요하다. REST 키는 이 경로에 넣지 않는다.

## 검증

- Red: `kakaoStationMap.spec.ts`에서 SDK 단일 요청·실패 재시도·마커/카드 선택의 assertion이 없는 구현에 실패했다.
- Green: SDK 로더, `StationMapSession` 카카오 어댑터, 키가 있을 때의 선택을 추가했다.
- 예외: SDK 다운로드 실패 또는 9초 내 준비되지 않는 응답은 지도 오류로 표시하고 다음 시도를 허용하며 목록은 유지한다. 키가 없는 환경은 기존 지도를 유지한다.
- 기존 지도 전용 단위·브라우저 검증은 키 없는 환경에서 계속 실행한다. 카카오 지도 실제 타일 및 도메인 허용은 운영 배포 후 별도로 확인한다.
- 첫 전체 verify는 배포 JAR 테스트 2개가 Leaflet 전용 선택자와 로컬 미등록 카카오 도메인을 사용해 종료 1이었다. 배포 JAR 테스트에 카카오 SDK mock을 추가해 같은 검색 영역·빈 결과 계약을 검사했다. 대상 배포 JAR 브라우저 4개 통과 후 SDK 준비 멈춤의 재시도 회귀 테스트와 9초 제한을 추가했다. 최종 전체 `CI=1 bash scripts/verify.sh` 종료 0: Java 458·프런트 404·개발 Chromium 39·배포 JAR 4·MySQL 8 및 재시작 JAR 7. 실행 로그: `/tmp/plugpass-kakao-map-verify-complete.log`, `/tmp/plugpass-kakao-runtime-2.log`.

이름·책임 검토: `loadKakaoMaps`는 SDK 다운로드/준비만, `createKakaoStationMap`은 지도·마커·영역 생명주기만 소유한다. `StationMap.vue`는 화면 상태와 사용자 이벤트를 유지하고, 검색 URL/API 계약은 바꾸지 않았다. 기존 Leaflet은 키 없는 개발 환경의 후퇴 경로로 남겼으며 실제 운영 카카오 지도를 확인한 뒤 제거한다.

공식 근거: [Kakao 지도 Web API 가이드](https://apis.map.kakao.com/web/guide/), [CustomOverlay 및 지도 이벤트](https://apis.map.kakao.com/web/documentation/).
