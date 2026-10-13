# 모바일 실사용 개선 실행 계획

> 실행: superpowers:executing-plans로 이 세션에서 직접 순차 구현한다. 병렬 에이전트는 사용하지 않는다.

**목표:** 위치/주소 선택부터 차량에 맞는 결과와 길찾기까지 모바일 사용 단계를 줄인다.

**구조:** 위치 요청은 composable, 확정 검색은 URL/store, 호환 판단은 정책, 사용자 문구는 presentation, 목적지 링크는 navigation 모듈이 소유한다. 기존 Spring Boot/JPA 조회·수집 경계를 유지한다.

**기술:** Java·Spring Boot·JPA/MySQL(운영)/H2(개발), Spring REST Docs, Vue3·TypeScript.

**설계:** [모바일 UX 개선 설계](mobile-ux-improvement-plan.md). 상태: 실행 계획 검토 대기.

## 공통 제약과 검토 초점

- 기능별 목적 이름 브랜치/worktree, TDD, 전체 verify, 서명 commit/PR/필수 CI/보호된 자동 머지. 배포 권한과 머지 권한을 구분한다.
- 기존 검색 URL·상세 복귀·추천이 다른 호환 조건을 사용하지 않도록 회귀 검증한다(T39/T42).
- 위치 허용 변경 후 이전 거부 결과가 최신 성공을 덮지 못하도록 검증한다(T38).
- 지도 SDK/타일 장애가 주소 선택과 목록 결과를 지우지 않도록 검증한다(T39).
- 지도 앱 미설치·저장소 접근 거부에서 웹 대안과 앱 재선택을 제공한다(T40).
- 모호한 제한·시간대 미확인·미제공 시각을 확정적인 이용 불가/최신으로 표현하지 않는다(T41).

## T38 위치 실패 복구·운영 예시 제거

대상: `frontend/src/shared/location/useCurrentLocation.ts`·동명 spec, `frontend/src/features/search/views/StationSearchView.vue`·동명 spec, `frontend/src/shared/ui/LoadingFlows.spec.ts`, 검색 View의 MapSearchView/PlaceSearchView spec, `frontend/e2e/{charging-journey,ui-failures,navigation,map-place-search}.spec.ts`.

계약: `requestLocation(): Promise<LocationCoordinates | null>`·status/error/coordinates/cancel은 유지한다. 브라우저 오류 문구를 임의 노출하지 않고 비보안 접속과 권한·획득·timeout을 구분한다. Permissions API의 사전 허용 여부로 요청을 영구 차단하지 않는다.

- [ ] Red: 권한 거부 후 두 번째 요청 성공 시 status=success/error=null/새 좌표, 비보안 접속 시 geolocation 미호출과 HTTPS 안내, 미지원·timeout·획득 실패 시 주소 검색 안내, 취소/늦은 callback 무시를 assertion한다.
- [ ] 실행: `npm --prefix frontend run test:unit -- src/shared/location/useCurrentLocation.spec.ts`. 신규 실패가 의도한 assertion인지 확인한다. 재시도 테스트가 이미 통과하면 기존 동작 확인으로 기록한다.
- [ ] Green: 위 composable의 환경 검사·오류 안내만 수정한다. View에서 example-location 버튼/설명/함수를 제거한다. 권한 실패 후 재시도 버튼과 주소 입력으로 이동하는 액션을 제공한다.
- [ ] View Red/Green: 예시 버튼 미노출, 주소 입력 포커스, 재시도/성공 검색 계약을 검증한다. 기존 예시 선택 fixture는 navigator 위치 경계 또는 URL의 명시 좌표로 전환한다.
- [ ] E2E: 기존 journey의 실제 DB/API assertion을 유지하고 browser context geolocation과 권한으로 위치 입력을 제어한다. 합성 공급자 좌표를 사용자 위치로 오인시키는 제품 버튼을 두지 않는다. ui-failures는 거부→주소 대안·재시도 성공을 별도 검증한다.
- [ ] Refactor 검토: 오류 분류와 문구는 위치 모듈, 포커스/사용자 행동은 View가 소유한다. 필요 없는 인터페이스를 추가하지 않는다.
- [ ] 전체 프런트·type-check/lint/build, cmux에서 headed `bash scripts/verify.sh`, 결과/로그 확인 후 기능 지도·T38 기록과 PR을 전달한다. iOS 실기기 성공 미확인은 별도 미완료로 둔다.

## T39 결과 우선·주소 지도 동기화

대상: StationSearchView.vue, PlaceSearchView.spec.ts, StationMapSelection.spec.ts, MapSearchView.spec.ts, frontend/src/styles/base.css, frontend/e2e/map-place-search.spec.ts·station-map.spec.ts.

계약: `selectPlace(place: PlaceMatch)`가 선택 좌표·지도 표시·확정 주변 검색을 조정한다. `stationSearchStore.search(criteria)`와 `toSearchQuery`를 재사용한다. 지도 이동은 자동 확정하지 않는다.

- [ ] Red: 후보 선택 좌표와 API/지도 중심 일치, 현재 차량 규격 유지, 오래된 주소/검색 응답 무시, 지도 오류 중 목록 유지를 검증한다.
- [ ] Green: 주소 선택에서 지도 표시/검색을 연결하고 검색 후 조건 편집은 접힌 요약으로 바꾼다. 결과 count와 첫 카드를 긴 설명 앞에 둔다. DOM 읽기 순서와 시각 순서를 일치시킨다.
- [ ] Red/Green E2E: 320/375px에서 검색 후 첫 카드의 bounding box가 viewport 안에 있고 키보드 순서가 조건 수정→결과로 이어지는지 확인한다. 빈 결과/오류에서는 해당 상태가 보이는지 확인한다.
- [ ] 전체 프런트·타입/lint/build·공용 verify와 기능 지도/기록 후 PR/CI/머지를 확인한다.

## T40 바로 길찾기·하단 선택 패널

대상: StationCard.vue·spec, StationSearchView.vue·StationMapSelection.spec, StationDirections.vue·StationNavigation.spec, `frontend/src/features/station-detail/navigation/navigationLinks.ts`, 새 `navigationPreference.ts`·spec 및 `StationActionSheet.vue`·spec, frontend/e2e/navigation.spec.ts.

계약: 기존 NavigationDestination·createNavigationLinks를 재사용한다. 선호 저장 함수는 `readNavigationPreference(): 'kakao' | 'naver' | null`, `saveNavigationPreference(app: 'kakao' | 'naver'): void`로 구성하며 저장소 접근 실패는 사용자 흐름을 막지 않는다. 패널은 destination과 close 이벤트를 갖고 링크 생성 정책은 소유하지 않는다.

- [x] Red: 카드 길찾기/마커 선택의 목적지 일치, 최초 앱 선택, 기억한 앱 직접 연결, 앱 변경, 잘못된 선호 값·저장소 예외를 검증한다.
- [x] Green: 목록에 직접 길찾기를 제공하고 마커 선택 시 하단 패널을 연다. 첫 선택에서 앱을 고르고 다음 클릭은 저장 앱 링크를 사용한다. 상세 화면의 details를 필수 길찾기 단계에서 제거한다.
- [x] Red/Green: Escape/닫기·초점 복원·탭 이동·목적지 교체·웹 대안·잘못된 좌표를 확인한다. 실제 앱 설치 여부를 브라우저에서 단정하지 않는다.
- [x] 기존 OS별 링크 테스트·개발/배포 navigation E2E·전체 verify와 [실행 기록](t40-verification.md). 실제 iOS 앱 전환은 실기기 검증으로 구분한다. PR/CI/머지는 별도 결과를 기록한다.

## T41 이용 조건·시각 요약

대상: ChargerList.vue·관련 spec, StationDetailView.vue·spec, charging-info/presentation.ts·spec, StationCard.vue·spec, StationSearchView.vue, frontend/e2e/charging-journey.spec.ts.

계약: API 원본 필드는 유지한다. presentation에 `accessConditionLabel(limitYn: string | null, limitDetail: string | null): string`을 추가해 구체 조건을 우선하고 Y만 있는 경우 확인 필요를 표시한다. 시각 표현은 sourceObservedAt·collectedAt·sourceStatusChangedAtRaw의 의미를 보존한다.

- [ ] Red: Y/N/누락·공백/모호한 제한·구체 조건·삭제 표시·빈 비고·시각 null/깨짐/미래를 검증한다. 기술 코드가 기본 요약에 없고 정보 출처 펼침에서 보이는지 assertion한다.
- [ ] Green: 상태·운영 시간·출입 조건을 먼저 표시하고 코드/식별자/수집 시각은 정보 출처로 옮긴다. 관측 미제공을 현장 확인 미제공으로 설명한다. 공급자 시각에 시간대를 추정해 붙이지 않는다.
- [ ] Refactor: 사용자 문구는 presentation에 모으고 서버 freshness/추천 정책을 UI가 재계산하지 않는다.
- [ ] 전체 프런트·개발/배포 E2E·공용 verify와 API 계약 유지 확인 후 기록/PR/CI/머지.

## T42 차량·현재 사용할 어댑터

대상: 새 frontend/src/features/vehicle/{vehicleCatalog.ts,vehicleCompatibility.ts,vehicleProfile.ts}·각 spec과 VehicleSelector.vue·spec, 검색 criteria/types/API/store/View·추천 API, Java search/recommendation의 request/query/service/repository와 기존 대응 테스트, REST Docs.

계약: VehicleSelector는 검증된 카탈로그 ID와 선택한 어댑터 ID를 전달한다. 차량 정보에는 시장·연식 구분과 공식 출처/확인일을 둔다. 정책 결과는 직접 연결 규격/선택 어댑터 규격/확인 필요를 구분한다. API는 connector(기존 단일 값)와 connectors(새 다중 값) 충돌을 400으로 거부하고 어느 것도 없으면 입력 오류로 처리한다. 지원 차량을 확정하기 전 공식 제조사 자료를 조사하며 불확실한 조합은 카탈로그에 넣지 않는다.

- [ ] Red: 정책의 직접/어댑터 미선택/사용 선택/미지원 조합·시장/연식 미확인, 프로필 저장 실패/오래된 카탈로그 ID를 검증한다.
- [ ] Green: 국내 검증 가능한 최소 카탈로그와 차량 입력·미등록 차량의 직접 규격 설정을 제공한다. 보유 어댑터를 자동 가정하지 않는다.
- [ ] Backend Red/Green: 다중 규격 합집합·같은 chargerId 중복 집계 방지·호환 필터 후 limit·추천 동일 정책·구 규격 URL을 실제 Repository/Service/HTTP 경계에서 검증한다. 상태/거리 정렬 계약은 유지한다.
- [ ] Frontend Red/Green: 차량 선택→API·URL·뒤로가기/상세/추천 조건 유지, 오류/응답 역전, 선택 표시 공간 고정·reduced motion을 검증한다.
- [ ] 공식 자료·REST Docs·기능 지도·필수 테스트 목록 갱신, 전체 Java/프런트/MySQL/공용 verify·cmux 개발/배포 브라우저 후 기록/PR/CI/머지.

## 실행 환경과 증거

현재 호출 확인: workspace:1000050003 / surface:1000050003. 보조 pane:1000050009가 있어 실행 전에 그 surface의 용도를 확인하고 재사용한다. 재개 시 ID를 다시 확인한다. 테스트 보고서·종료 코드와 실패 원인을 기능별 docs/tNN-verification.md에 기록한다. 문서 검토는 제품 테스트 통과로 보고하지 않는다.
