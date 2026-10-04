# PlugPass 작업 지침

## 적용 범위와 프로젝트 기준

- 기능 추가·버그 수정·동작 변경에 이 지침을 적용한다. 기존 코드의 관례나 과거 계획과 충돌하면 최신 사용자 지시와 이 지침을 기준으로 관련 계획을 갱신한다.
- 기술 스택은 Java, Spring Boot, JPA·H2, Spring REST Docs의 큰 줄기로 설명한다. 전이 라이브러리·빌드 도구를 스택 표에 늘어놓지 않으며 실행 명령은 안내한다.
- [프로젝트 계획](plan.md), [작업 체크리스트](tasks.md), [기능 지도](docs/feature-map.md), [verify-plugpass](.agents/skills/verify-plugpass/SKILL.md)를 재사용한다.
- agent-engineering 적용 시 [검증 계획](.agents/verification.json)으로 기존 공용 verify를 실행하고 현재 소스와 로그를 연결할 수 있다. 사용법과 실제 적용 범위는 [적용 기록](docs/agent-engineering-application.md)을 따른다. CI는 저장소의 `scripts/verify.sh`를 사용한다.
- T18부터 `frontend/`의 Vue·TypeScript 코드에 프런트 규칙을 적용한다. 구현 완료 범위와 실제 검증은 tasks.md·기능 지도의 최신 근거로 판단한다.
- 이 문서는 개발·리뷰 기준이다. 문서를 추가했다는 이유로 기존 코드 전체가 준수하거나 자동 검사기가 강제한다고 보고하지 않는다.

## 공식 문서와 설명

- 구현·수정할 기술은 설정과 실제 의존성 해석 결과의 버전을 먼저 확인한다. [공식 문서 학습 기록](docs/official-stack-guide.md)과 [출처 목록](docs/official-sources.json)을 읽고 관련 reference/API를 확인한다.
- 블로그나 기억만으로 다른 버전의 예제를 옮기지 않는다. rolling 문서는 현재 버전과 차이를 확인한다. 기술 도입·버전 변경 시 역할·공식 출처·동작 차이·검증 방법을 학습 기록에 갱신한다.
- 공식 동작과 프로젝트의 설계 판단을 구분한다. 무엇이 달라지는지, 왜 필요한지, 책임과 데이터 흐름, 성공·실패 조건을 직관적으로 설명하고 필요한 경우 도식화한다.
- 비단순 구현·수정·리팩터링·에이전트 작업 환경 구축에는 전역 `agent-engineering` 스킬을 적용한다. 기존 검사·검증 도구를 우선 사용하고 중복 체계를 만들지 않는다.

## TDD: Red → Green → Refactor

모든 기능 추가·버그 수정·동작 변경은 테스트를 먼저 작성한다. 구현 후 테스트를 덧붙이는 방식은 TDD로 간주하지 않는다.

```text
가장 작은 관찰 가능한 동작 정의
  → RED: 테스트 작성·실행 → 누락된 동작 때문에 assertion 실패 확인
  → GREEN: 통과에 필요한 최소 구현 → 대상 테스트 → 전체 테스트
  → REFACTOR: 동작 유지·책임/이름/중복 개선 → 전체 테스트
```

1. 요구사항을 한 번에 검증할 수 있는 작은 동작으로 나눈다. 테스트 하나는 동작 하나 또는 실패 원인 하나를 설명한다.
2. 프로덕션 코드를 바꾸기 전에 실패 테스트를 작성하고 실제 실행한다. 실행하지 않은 테스트 파일은 Red도 통과도 아니다.
3. 컴파일·문법·환경·fixture·테스트 실수는 기능 Red가 아니다. 원인을 바로잡고 의도한 동작의 assertion 실패를 확인한다.
4. Green에서는 현재 실패를 해결하는 최소 구현만 작성한다. 예상 확장을 선구현하지 않고 대상·전체 테스트를 실행한다.
5. Green에서만 리팩터링한다. 동작 변경을 섞지 않고 책임·이름·중복을 개선한 이유를 설명한 뒤 전체 테스트를 다시 실행한다.
6. 버그 회귀 테스트가 처음부터 통과하면 기존 동작을 확인한 것이다. 재현 조건·요구사항을 점검하며 assertion을 제거하거나 느슨하게 만들지 않는다.
7. 테스트 선행이 기술적으로 불가능하면 임의로 건너뛰지 않는다. 이유·위험을 밝히고 가장 가까운 실행 가능한 검증 방법에 사용자와 합의한 뒤 진행한다.

## 성공·예외·실패·엣지 케이스

성공 흐름만 검증하고 완료하지 않는다. 변경한 동작마다 정상 결과와 관련된 예외·실패·경계 조건을 식별하고 실제 테스트로 확인한다.

| 구분 | 검토할 입력·상황 | 확인할 결과 |
| --- | --- | --- |
| 정상 | 최소 유효 입력·일반 입력 | 반환값·저장 결과·필요한 협력 계약 |
| 경계 | 최솟값/최댓값과 바로 밖, 빈 결과, null/누락, 시각 경계 | 허용·거부 기준과 정확한 내용/건수 |
| 도메인 실패 | 중복·금지 상태 전이·없는 대상·잘못된 관계 | 의도한 예외, 기존 필드·시각 유지 |
| 외부 실패 | timeout·인증 오류·429·5xx·깨진 응답 | 오류 분류·재시도 상한·기존 데이터 보호·복구 |
| 순서·격리 | 반복 호출·응답 역전·동시 실행·이전 테스트의 상태 | 멱등성·순서 규칙·상태 오염 방지 |

- 해당하지 않는 범주는 적용하지 않는 이유를 기록한다. 의미 없는 테스트 수를 늘리거나 모든 실패를 하나의 조건 분기 테스트에 넣지 않는다.
- 예외는 발생 여부뿐 아니라 필요한 타입·공개 메시지/오류 코드와 실패 후 상태를 검증한다. HTTP 상태·validation 메시지는 HTTP 경계에서 검증한다.

## 객체 책임과 협력

```text
Controller ── HTTP 변환 ──► Application Service ── 상태 규칙 ──► Domain
                                  └── 저장·외부 연동 경계 ──► DB·외부 API

View/Component ── 사용자 이벤트 ──► Store/상태 관리 ──► API 모듈
       ▲                                │
       └──────── props/state ────────────┘
```

- Controller는 HTTP 입출력 변환을 맡고 비즈니스 판단을 소유하지 않는다. HTTP DTO와 도메인 모델을 분리한다.
- Application Service는 유스케이스 흐름·권한·트랜잭션을 조정한다. 도메인 규칙을 거대한 조건문으로 독점하지 않는다.
- Domain은 자신의 상태와 불변식을 지킨다. DTO·Service를 우회해도 도메인의 최종 방어가 유지돼야 한다.
- 파일·외부 API 등 변동 지점은 작은 인터페이스 뒤에 둔다. 모든 객체에 추측성 인터페이스를 만들지 않는다. 아래 Service 인터페이스는 명시적으로 선택한 유스케이스 계약이다.
- View는 페이지 조립·라우팅, 표현 컴포넌트는 props/event, 상태 관리는 상태 전이·비동기 흐름, API 모듈은 Axios 호출을 맡는다.
- 공개 동작과 결과를 검증한다. private 메서드 호출 횟수처럼 리팩터링에 따라 바뀌는 내부 구현에 테스트를 결합하지 않는다.
- [객체지향 생활 체조](docs/object-calisthenics.md)를 적용한다. 중첩·else와 내부 연쇄 탐색을 줄이고 값·컬렉션의 규칙을 소유 객체에 모은다. 객체 크기·필드 수는 책임 검토 기준이며 숫자만 맞추는 래퍼·분리는 금지한다.
- JPA·DTO·설정·테스트·빌더의 예외는 이유와 보완 검증을 설명한다. 아래 읽기 getter 허용은 상태 판단을 외부로 옮기거나 public setter를 허용한다는 뜻이 아니다.

## Java·도메인·엔티티

- 운영·테스트 모두 명시적 Java 타입을 사용한다. `var`를 쓰지 않는다. 타입의 완전 수식 패키지명은 코드 본문 대신 import에 둔다.
- JPQL enum 조건은 완전 수식 enum 리터럴 대신 파라미터로 전달한다. enum 저장은 특별한 이유가 없으면 `@Enumerated(EnumType.STRING)`을 사용한다.
- 상태 변경은 `update`, `publish`, `retire`처럼 의도가 드러나는 메서드로 제공한다. **전체 입력 검증 → 필드 변경 → updatedAt 갱신** 순서로 실행하고 검증 실패 시 모든 필드와 시각을 유지한다.
- JPA 엔티티 기본 형태는 Lombok `@Getter`, public setter 없음, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`다. 외부 생성은 값을 받는 `private` 생성자에 `@Builder`를 적용한다.
- 빌더에는 외부에서 결정하는 값만 노출한다. 초기 상태·유형·출처는 생성자가 결정한다. 생성자 내부에서도 불변식을 검증한다.
- 현재 Lombok은 미도입이다. 해당 형태를 처음 구현하는 작업에서 공식 호환성·annotation processor 구성을 확인하고 의존성 및 컴파일 검증을 함께 반영한다.
- 생성 시 `createdAt`과 `updatedAt`은 같은 시각이다. 수정 성공 시 `createdAt`을 유지하고 `updatedAt`만 갱신한다. 시각 생성을 `@PrePersist`·`@PreUpdate`에 위임하지 않는다. 시간은 제어 가능한 경계를 통해 전달한다.
- API는 Entity를 직접 반환하지 않고 응답 DTO로 변환한다.

## Service와 HTTP 경계

- Service는 `{ServiceName}` 인터페이스와 `Default{ServiceName}` 구현으로 구성한다. `{ServiceName}Impl` 이름은 금지한다.
- 인터페이스는 유스케이스 계약을, 구현은 `@Service`, production `@Transactional`, Repository 의존성과 유스케이스 조정을 소유한다. 호출자·테스트는 Service 인터페이스에 의존한다.
- 트랜잭션은 실제 저장·조회 경계에 둔다. 외부 네트워크 대기를 긴 DB 트랜잭션에 포함하지 않고 페이지 저장 등 필요한 단위에서 commit한다. 도메인이 판단할 규칙을 Service에만 두지 않는다.
- 기능별 패키지 아래 `controller`, `service`, `domain`, `repository`, `dto`로 책임을 구분한다. 없는 역할의 빈 패키지는 만들지 않는다.
- 요청 DTO는 해당 기능의 `dto.request`, 응답 DTO는 `dto.response`에 둔다. Service의 전달 모델은 `dto`, 공용 HTTP DTO는 `common.dto.request`·`common.dto.response`로 구분한다. 설정은 `config`, 외부 통신은 `client`, 스케줄 진입점은 `scheduler`에 둔다.
- 여러 기능이 공유하는 업무 규칙은 소유 도메인에 두고, 공유 입력 검증은 `common.validation`에 둔다.
- 입력 필수값·범위·형식·validation 메시지는 request DTO가 소유한다. Controller에 규칙·메시지를 분산하지 않는다.
- 사용자 정의 예외는 프로젝트 루트에 맞는 `com.plugpass.exception`에 둔다. 공통 예외 처리기는 애플리케이션 예외를 HTTP 오류 응답으로 변환한다.

## 백엔드 테스트 경계와 명령

- 전체 테스트: `./gradlew test`
- 빠른 반복: `./gradlew test --tests '패키지.클래스명'`
- 패키징·실제 HTTP까지 필요한 공용 검증: `bash scripts/verify.sh`

| 대상 | 기본 검증 방식 | 경계 |
| --- | --- | --- |
| Domain | Spring·DB 없는 순수 단위 테스트 | 실제 도메인 규칙을 fake로 대체하지 않음 |
| Repository | `@DataJpaTest`, 실제 매핑·쿼리 | 기본 save 후 조회, 관례적 flush/clear 없음 |
| Service | `@SpringBootTest` + 실제 Repository/DB | production 트랜잭션 commit, 테스트 `@Transactional`·Service mock 금지 |
| Controller / REST Docs | `@WebMvcTest` + Spring 구성 MockMvc | Service·직접 의존 하위 계층은 `@MockitoBean`, 실제 DB·cleanup 없음 |
| 핵심 사용자 흐름 | 소수의 별도 API/전체 통합 테스트 | Controller부터 실제 저장·조회까지 연결 |

- 현재 Spring Boot 4.1/Spring Framework 7의 Bean 대체는 `@MockitoBean`을 사용한다. 요청의 `@MockBean` 역할을 현재 API에 맞춘 것이다. [WebMvcTest 공식 API](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/webmvc/test/autoconfigure/WebMvcTest.html), [MockitoBean 공식 문서](https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-mockitobean.html).
- MVC 테스트는 실제 Spring 바인딩·validation·직렬화 구성을 사용한다. `standaloneSetup`으로 대체하지 않고 대상 Controller 등 필요한 MVC 구성만 지정하여 JPA 설정을 로딩하지 않는다.
- Service 테스트에서 `@Mock`, `@InjectMocks`, `@MockBean`, `@MockitoBean`은 사용하지 않는다. 실제 Service와 Repository를 호출하며 외부 I/O·시간만 작은 경계의 테스트 구현으로 제어한다.
- Service 저장 변경은 **서비스 호출 종료·commit → 별도 Repository 재조회**로 검증한다. 테스트 트랜잭션이 production 트랜잭션 누락을 가리지 않게 한다.
- JPA 저장은 기본 `save()`다. SQL 즉시 실행이 검증 대상일 때만 `flush()`/`saveAndFlush()`를 사용한다. Repository query fixture에 관례적인 `EntityManager.clear()`를 넣지 않는다.
- 매핑 복원이 검증 대상일 때만 이유를 드러내고 `flush()` + `clear()`를 사용한다. 객체 참조 차이(`isNotSameAs`) 자체는 검증하지 않는다.
- test-level `@Transactional`이 필요한 JPA/API 테스트는 rollback으로 검증 목적이 가려지지 않는지 확인한다. Service 테스트의 금지를 우회하는 용도로 사용하지 않는다.
- Controller의 Service 반환 fixture는 mock 객체로 준비한다. ID 주입을 위한 `ReflectionTestUtils`를 쓰지 않는다. 실제 응답 JSON 값과 필요한 Service 요청 전달 계약을 검증한다.
- JSON 요청은 주입받은 `ObjectMapper`로 요청 객체를 직렬화한다. 원문 JSON은 문법 오류처럼 객체로 표현할 수 없는 요청에만 사용한다. REST Docs는 문서 생성만으로 성공 판정하지 않는다.
- HTTP 계약·validation·직렬화는 MockMvc로 검증한다. 실제 보안 필터는 필요한 최소 범위의 Spring Security Test로 검증하며, 보안 기능이 없는 현재 프로젝트에 테스트 목적만으로 도입하지 않는다.

## 테스트 이름·준비·assertion

- 테스트는 빠르고 독립적이며 반복 가능하고 자동 판정 가능해야 한다. 본문은 준비 → 실행 → 검증이 보이게 작성한다.
- 메서드명은 영어 Java 관례를 따른다. 모든 테스트 메서드에 자연스러운 한글 `@DisplayName`을 쓰고 테스트 클래스에는 붙이지 않는다.
- 애너테이션 순서는 `@Test` → `@DisplayName`, 매개변수 테스트는 `@ParameterizedTest` → `@DisplayName` → `@MethodSource` 등 인자 제공 애너테이션이다.
- 도메인 객체 준비는 각 본문에서 명시한다. 관계·성공/실패 조건·검색 조건·경계값을 숨기지 않고 해당 동작에 필요한 최소 개수만 생성한다.
- 여러 객체를 묶는 Fixture class/record, `fixture()` 일괄 준비 helper, 이름만 바꾼 시나리오 묶음 helper는 금지한다. 외부 응답 파일 fixture와는 구분한다.
- 반복 생성 helper는 테스트 클래스 내부 private 메서드로 한 종류 객체의 생성·저장까지만 맡긴다. 본문에 호출을 드러내고 매번 새 데이터를 만든다.
- `@BeforeEach`는 MockMvc 구성 같은 실행 준비에만 사용한다. 핵심 실행·검증·일부 테스트 전용 stubbing·이전 데이터 cleanup을 넣지 않는다.
- 지역 변수는 도메인 대상·역할을 드러낸다. 반복 index 외 한 글자 이름, boolean 테스트 매개변수·boolean fixture helper를 쓰지 않는다.
- 준비·실행·기대 결과가 갈리는 `if`는 별도 테스트로 나눈다. timeout·동시성 제어 같은 기반 코드에만 조건 분기를 허용한다.
- 반복 횟수·데이터 규모 자체가 요구사항일 때만 `for`를 사용한다. 독립 입력 검증은 매개변수 테스트로 표현한다.
- Fake는 시계·외부 시스템처럼 제어할 조건만 대체하고 실제 도메인 규칙은 유지한다. 상태 있는 Fake는 테스트 사이에 공유하지 않는다.
- 조회 결과는 가능한 한 전체 원소를 검증한다. 정렬 계약이 없으면 순서를 가정하지 않는다. 페이지·슬라이스는 내용·실제 내용 개수와 전체 개수 또는 `hasNext`까지 검증한다.
- 단순 DTO의 자체 단위 테스트는 생략할 수 있다. validation·변환·기본값·정규화·계산 동작은 테스트한다. JSON 이름·직렬화·validation 메시지·HTTP 상태는 Controller/API 테스트가 맡는다.
- 로컬 seed SQL은 수동 화면 확인 전용이다. 자동화 테스트나 건수 assertion이 의존하지 않게 한다.

## 테스트 격리와 teardown

```text
실제 Service 호출·production transaction commit
  → 별도 Repository 재조회·결과 검증
  → @AfterEach: 자식 → 부모 순서로 데이터 삭제
```

- 정리는 데이터를 만든 테스트 클래스가 소유한다. 해당 테스트가 실제 생성하는 엔티티의 Repository만 주입한다.
- Service 테스트는 `@AfterEach`에서 FK 역순으로 정리한다. `@BeforeEach` cleanup은 금지하며 assertion 실패·부분 데이터 생성 후에도 정리 가능한 순서를 사용한다.
- `deleteAllInBatch()`는 cascade를 실행하지 않으므로 연결 테이블부터 삭제한다. 소유 자식·element collection 정리가 필요한 aggregate는 `deleteAll()`, cascade/callback이 필요 없는 단순 테이블은 FK 역순 `deleteAllInBatch()`를 사용한다.
- bulk 삭제 후 같은 영속성 컨텍스트의 엔티티를 재사용하지 않는다. 정리를 위해 test-level transaction이나 EntityManager 의존성을 추가하지 않는다.
- 공통 테스트 부모에 Repository·fixture·teardown을 일괄 상속하지 않는다. 각 클래스가 의존성과 정리를 직접 소유한다.
- 공유 DB의 병렬 테스트에서 전체 `deleteAll*()`을 사용하지 않는다. 테스트별 데이터·schema 격리 또는 직렬화가 필요하다.
- 외부 시스템·시간·난수·파일·네트워크를 경계 뒤로 격리한다. 운영 DB·S3·테스트 실행 순서에 의존하지 않는다.
- static·singleton·ThreadLocal·보안 컨텍스트·시스템 속성·파일은 사용 전 상태로 복원한다. Spring singleton 재생성이 필요한 클래스에만 `@DirtiesContext`를 사용한다. 매 테스트 새로 만든 일반 객체에는 불필요한 teardown을 추가하지 않는다.

## 프런트엔드 테스트 경계

- 테스트 명령이 없다면 기능 구현 전에 Vue 버전에 맞는 러너·DOM 환경·컴포넌트 도구를 마련한다. 최소 예제의 실제 실패·통과를 확인하고 표준 명령을 저장소에 기록한다.
- 내부 변수·private 구현보다 렌더링·입력·클릭·이동·props·emitted event 계약을 검증한다. store의 상태 전이와 비동기 성공·실패는 독립적으로 검증하고 라우터·Axios를 경계에서 제어한다.
- 로딩·빈 결과·성공·유효성 오류·서버 오류를 구분하며 재시도·응답 순서 역전을 해당 동작의 회귀 사례에 포함한다.
- 변경 후 전체 테스트와 lint를 실행하고 배포 산출물에 영향이 있으면 build도 확인한다. 실행하지 않은 테스트를 Red나 통과로 보고하지 않는다.

## 이름·책임·배치의 상시 검토

모든 코드 작업은 변경한 코드와 호출부를 **이름 → 실제 수행 내용 → 클래스 배치·추출 여부** 순서로 재검토한다. Java·JavaScript·TypeScript·Vue 코드 모두에 적용한다.

- 클래스·메서드·필드·매개변수·지역 변수는 도메인 대상과 역할을 드러낸다. `getBuild`, `setting*`, `pageTest`처럼 모호한 이름은 실제 계약에 맞춘다.
- Repository 인스턴스는 `xxxRepository`로 이름 짓고 엔티티 컬렉션처럼 보이는 복수 이름을 쓰지 않는다. Service·Port·Policy·Client·Mapper·Provider도 대상과 역할을 포함한다.
- 메서드 이름은 조회·변경·저장·외부 호출·예외 같은 실제 효과와 일치해야 한다. 여러 단계가 하나의 유스케이스 조정인지 먼저 판단하고 다른 규칙·변경 이유가 섞였을 때만 책임을 분리한다.
- 읽는 상태·바꾸는 상태·판단 정보를 소유한 클래스에 메서드가 있는지 살핀다. 함께 바뀌는 메서드들이 독립된 개념이면 별도 클래스 추출을 검토한다. 추출할 객체의 책임·입출력·의존성을 설명하고 줄 수·호출 개수만으로 분리하지 않는다.
- 이름·배치·책임을 바꾸면 선언부·호출부·테스트와 JSON 필드·DB 컬럼·라우트·직렬화 등 공개 계약의 영향도 확인한다. 단순 위임 클래스나 추측성 인터페이스를 만들지 않는다.
- SOLID는 Java와 독립된 TypeScript 클래스·모듈에 적용한다. Vue 컴포넌트 자체에는 SOLID 판정을 강제하지 않고 props/event·렌더링·상태·API 경계로 평가한다. TypeScript가 없으면 적용 대상 없음으로 기록한다.
- 검토 근거, 조치 또는 유지 이유, 호출부·공개 계약 검증 결과를 작업 기록/PR에 남긴다.

## 로컬 E2E와 작은 PR

- 로컬 E2E는 현재 cmux에서 실행 과정을 보여 준다. `CMUX_WORKSPACE_ID`, `CMUX_SURFACE_ID`, `cmux identify --json`으로 호출 workspace·surface를 확인한다. 호출 정보가 없으면 확인한 포커스 기준을 사용자에게 알린다.
- 같은 workspace의 검증 보조 pane을 재사용하고 없으면 호출 터미널 오른쪽에 하나 만든다. 대상 workspace·surface를 명시하고 지원되는 생성 명령은 `--focus false`를 사용한다. 사용자가 입력 중인 터미널을 사용하지 않는다.
- 저장소 경로·검증 흐름을 먼저 설명하고 러너·서버 명령과 로그를 보인다. 웹 흐름은 같은 workspace의 cmux 브라우저에서 실제 클릭·입력·이동을 수행한다. 기존 자동 테스트는 유지하고 가능한 headed/UI 모드로 실행한다.
- 실제 러너와 cmux 브라우저의 mock·기능 차이를 설명한다. 미리보기·보고서를 실제 자동화 화면으로 표현하거나 테스트를 약화해 통과 처리하지 않는다.
- cmux가 없거나 연결/기능 제한이 있으면 원인·대체 검증 범위를 명시한다. 종료 코드·assertion·실패 원인·로그/보고서를 확인하고 검증 pane은 유지한다. 다른 workspace의 pane·포커스를 임의로 변경하지 않는다. CI headless에는 로컬 표시 규칙을 강제하지 않는다.
- 승인된 작업 범위에서는 [전달 절차](docs/delivery-workflow.md)에 따라 검증 → 서명 commit/push → PR 생성 → 보호 규칙 아래 native auto-merge 신청 → 실제 머지 확인까지 진행한다. 테스트·문서는 해당 동작 PR에 함께 넣는다.
- main 직접 push는 최초 부트스트랩에만 사용한다. 보호 규칙을 확인하지 못하면 PR까지만 진행한다. 배포·유료 요금제 변경·공개 전환·병렬 에이전트 실행 권한은 추가하지 않는다.

## 완료 판정과 기록

- 코드 작업 결과에는 Red의 실제 명령·의도한 실패 원인, Green의 최소 변경, Refactor의 구조 개선·이유(불필요하면 유지 이유), 대상·전체 검증 결과를 남긴다.
- 성공 흐름뿐 아니라 관련 예외·실패·엣지 케이스의 검증 결과와 적용하지 않은 항목의 이유를 기록한다. 이름·책임·호출부·공개 계약 검토 결과도 포함한다.
- 테스트를 작성하지 않았거나 실행하지 않은 기능 변경을 완료했다고 보고하지 않는다. 기존 실패도 원인을 밝히고 관련성·영향을 설명하며 실행 불가를 통과로 취급하지 않는다.
- 컴파일·테스트·lint·build의 필수 검증이 실패하면 배포 가능하다고 보고하지 않는다. 테스트 0개·전체 skip·이전 결과·컴파일 성공만으로 사용자 흐름의 성공을 주장하지 않는다.
- 문서만 변경하면 링크·참조·내용 정합성을 검증한다. 불필요한 애플리케이션 테스트·검사 체계를 추가하지 않고, 문서 검사를 애플리케이션 테스트 실행으로 표현하지 않는다. 원격 필수 CI 결과는 별도로 기록한다.
