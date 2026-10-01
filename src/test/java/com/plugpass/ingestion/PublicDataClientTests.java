package com.plugpass.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;
import com.plugpass.station.ChargerId;
import com.plugpass.station.ChargerStatus;
import com.plugpass.station.GeoPoint;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class PublicDataClientTests {
    private static final String SERVICE_KEY = "fixture-secret+/=";
    private static final Instant COLLECTED_AT = Instant.parse("2026-10-01T00:00:00Z");
    private HttpServer server;
    private ExecutorService serverExecutor;
    private PublicDataClient client;

    @BeforeEach
    void startFixtureServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverExecutor = Executors.newSingleThreadExecutor();
        server.setExecutor(serverExecutor);
        server.start();
    }
    @AfterEach
    void stopOwnedResources() {
        if (client != null) {
            client.close();
        }
        server.stop(0);
        serverExecutor.shutdownNow();
    }
    private PublicDataProperties properties(String serviceKey, Duration responseTimeout) {
        return new PublicDataProperties(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/getChargerInfo"),
                serviceKey, 10, "28", Duration.ofSeconds(1), responseTimeout);
    }
    private static String readFixture(String filename) throws IOException {
        try (InputStream input = PublicDataClientTests.class.getResourceAsStream("/publicdata/" + filename)) {
            if (input == null) {
                throw new IllegalArgumentException("missing fixture");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/xml; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        } finally {
            exchange.close();
        }
    }

    @Test
    @DisplayName("실제 HTTP 응답을 식별자·위치·상태·원본 운영 정보로 변환한다")
    void convertsNormalResponse() throws IOException {
        String body = readFixture("normal.xml");
        server.createContext("/getChargerInfo", exchange -> respond(exchange, 200, body));
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofSeconds(2)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));

        StationPage page = client.fetchPage(1);

        assertThat(page.pageNumber()).isEqualTo(1);
        assertThat(page.totalCount()).isEqualTo(1);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.snapshots()).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.chargerId()).isEqualTo(new ChargerId("ME", "28260005", "02"));
            assertThat(snapshot.stationName()).isEqualTo("기후대기관");
            assertThat(snapshot.location()).isEqualTo(new GeoPoint(37.569620, 126.641973));
            assertThat(snapshot.status()).isEqualTo(ChargerStatus.AVAILABLE);
            assertThat(snapshot.rawStatus()).isEqualTo("2");
            assertThat(snapshot.sourceObservedAt()).isNull();
            assertThat(snapshot.collectedAt()).isEqualTo(COLLECTED_AT);
            assertThat(snapshot.details().connectorCode()).isEqualTo("03");
            assertThat(snapshot.details().useTime()).isEqualTo("24시간 이용가능");
            assertThat(snapshot.details().limitYn()).isEqualTo("N");
            assertThat(snapshot.details().limitDetail()).isEmpty();
            assertThat(snapshot.details().note()).isEqualTo("공사로 인해 이용 불가");
            assertThat(snapshot.details().sourceStatusChangedAtRaw()).isEqualTo("20190829121020");
            assertThat(snapshot.details().lastChargingStartedAtRaw()).isEqualTo("20210801121020");
            assertThat(snapshot.details().lastChargingEndedAtRaw()).isEqualTo("20210801123020");
            assertThat(snapshot.details().chargingStartedAtRaw()).isEqualTo("20210802131020");
            assertThat(snapshot.details().delYn()).isEqualTo("N");
            assertThat(snapshot.details().delDetail()).isEmpty();
        });
        assertThatThrownBy(() -> page.snapshots().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test
    @DisplayName("기관 계약의 키·페이지·지역 파라미터를 URL 인코딩해 전달한다")
    void sendsEncodedParameters() throws IOException {
        AtomicReference<String> query = new AtomicReference<>();
        String body = readFixture("normal.xml");
        server.createContext("/getChargerInfo", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            respond(exchange, 200, body);
        });
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofSeconds(2)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));

        client.fetchPage(1);

        assertThat(query.get()).isNotNull();
        Map<String, String> parameters = Arrays.stream(query.get().split("&"))
                .map(parameter -> parameter.split("=", 2))
                .collect(Collectors.toMap(pair -> pair[0], pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
        assertThat(parameters).containsExactlyInAnyOrderEntriesOf(Map.of("serviceKey", SERVICE_KEY, "pageNo", "1", "numOfRows", "10", "zcode", "28", "dataType", "XML"));
    }
    @Test
    @DisplayName("빈 페이지는 빈 결과와 다음 페이지 없음으로 반환한다")
    void returnsEmptyPage() throws IOException {
        String body = readFixture("empty.xml");
        server.createContext("/getChargerInfo", exchange -> respond(exchange, 200, body));
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofSeconds(2)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));

        StationPage page = client.fetchPage(1);

        assertThat(page.snapshots()).isEmpty();
        assertThat(page.totalCount()).isZero();
        assertThat(page.hasNext()).isFalse();
    }
    @Test
    @DisplayName("전체 건수가 현재 페이지 범위를 넘으면 다음 페이지가 있다")
    void identifiesNextPage() throws IOException {
        String body = readFixture("normal.xml").replace("<totalCount>1</totalCount>", "<totalCount>11</totalCount>");
        server.createContext("/getChargerInfo", exchange -> respond(exchange, 200, body));
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofSeconds(2)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));

        StationPage page = client.fetchPage(1);

        assertThat(page.snapshots()).hasSize(1);
        assertThat(page.totalCount()).isEqualTo(11);
        assertThat(page.hasNext()).isTrue();
    }
    @Test
    @DisplayName("마지막 페이지는 실제 결과를 보존하며 다음 페이지 없음으로 반환한다")
    void identifiesLastPage() throws IOException {
        String body = readFixture("normal.xml").replace("<pageNo>1</pageNo>", "<pageNo>2</pageNo>").replace("<totalCount>1</totalCount>", "<totalCount>11</totalCount>");
        server.createContext("/getChargerInfo", exchange -> respond(exchange, 200, body));
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofSeconds(2)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));

        StationPage page = client.fetchPage(2);

        assertThat(page.pageNumber()).isEqualTo(2);
        assertThat(page.snapshots()).hasSize(1);
        assertThat(page.hasNext()).isFalse();
    }
    @Test
    @DisplayName("미지원 상태는 UNKNOWN으로 변환하고 원본 코드를 보존한다")
    void preservesUnknownStatus() throws IOException {
        String body = readFixture("unknown-status.xml");
        server.createContext("/getChargerInfo", exchange -> respond(exchange, 200, body));
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofSeconds(2)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));

        assertThat(client.fetchPage(1).snapshots()).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.status()).isEqualTo(ChargerStatus.UNKNOWN);
            assertThat(snapshot.rawStatus()).isEqualTo("9");
        });
    }
    @Test
    @DisplayName("누락된 상태를 UNKNOWN으로 처리하며 관측 시각을 생성하지 않는다")
    void preservesMissingStatus() throws IOException {
        String body = readFixture("normal.xml").replace("<stat>2</stat>", "");
        server.createContext("/getChargerInfo", exchange -> respond(exchange, 200, body));
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofSeconds(2)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));

        assertThat(client.fetchPage(1).snapshots()).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.status()).isEqualTo(ChargerStatus.UNKNOWN);
            assertThat(snapshot.rawStatus()).isNull();
            assertThat(snapshot.sourceObservedAt()).isNull();
        });
    }
    @ParameterizedTest
    @DisplayName("HTTP 오류를 인증·호출 한도·서버·계약 실패로 구분하고 키를 숨긴다")
    @MethodSource("httpFailures")
    void classifiesHttpFailure(int status, PublicDataFailure expected, CapturedOutput output) {
        server.createContext("/getChargerInfo", exchange -> respond(exchange, status, "<error>" + SERVICE_KEY + "</error>"));
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofSeconds(2)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));

        assertThatThrownBy(() -> client.fetchPage(1)).isInstanceOf(PublicDataException.class)
                .hasMessage("Public data request failed: " + expected).hasNoCause()
                .satisfies(error -> assertThat(((PublicDataException) error).getFailure()).isEqualTo(expected));
        assertThat(output.getAll()).doesNotContain(SERVICE_KEY, URLEncoder.encode(SERVICE_KEY, StandardCharsets.UTF_8));
    }
    private static Stream<Arguments> httpFailures() {
        return Stream.of(Arguments.of(401, PublicDataFailure.AUTHENTICATION), Arguments.of(403, PublicDataFailure.AUTHENTICATION),
                Arguments.of(429, PublicDataFailure.RATE_LIMIT), Arguments.of(500, PublicDataFailure.SERVER),
                Arguments.of(503, PublicDataFailure.SERVER), Arguments.of(404, PublicDataFailure.CONTRACT));
    }
    @ParameterizedTest
    @DisplayName("HTTP 200이어도 공급자의 오류 코드가 있으면 실패로 반환한다")
    @MethodSource("providerFailures")
    void rejectsProviderErrorCode(String resultCode, PublicDataFailure expected) throws IOException {
        String body = readFixture("normal.xml").replace("<resultCode>00</resultCode>", "<resultCode>" + resultCode + "</resultCode>");
        server.createContext("/getChargerInfo", exchange -> respond(exchange, 200, body));
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofSeconds(2)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));

        assertThatThrownBy(() -> client.fetchPage(1)).isInstanceOf(PublicDataException.class)
                .satisfies(error -> assertThat(((PublicDataException) error).getFailure()).isEqualTo(expected));
    }
    private static Stream<Arguments> providerFailures() {
        return Stream.of(Arguments.of("20", PublicDataFailure.AUTHENTICATION), Arguments.of("30", PublicDataFailure.AUTHENTICATION),
                Arguments.of("31", PublicDataFailure.AUTHENTICATION), Arguments.of("22", PublicDataFailure.RATE_LIMIT),
                Arguments.of("23", PublicDataFailure.RATE_LIMIT), Arguments.of("05", PublicDataFailure.TIMEOUT),
                Arguments.of("01", PublicDataFailure.SERVER), Arguments.of("04", PublicDataFailure.CONTRACT), Arguments.of("99", PublicDataFailure.CONTRACT));
    }
    @ParameterizedTest
    @DisplayName("깨진 XML·누락 식별자·잘못된 좌표·페이지 계약·외부 entity를 거부한다")
    @MethodSource("invalidResponses")
    void rejectsInvalidResponse(String body, CapturedOutput output) {
        server.createContext("/getChargerInfo", exchange -> respond(exchange, 200, body));
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofSeconds(2)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));

        assertThatThrownBy(() -> client.fetchPage(1)).isInstanceOf(PublicDataException.class)
                .hasMessage("Public data request failed: CONTRACT").hasNoCause();
        assertThat(output.getAll()).doesNotContain(SERVICE_KEY);
    }
    private static Stream<String> invalidResponses() throws IOException {
        String normal = readFixture("normal.xml");
        return Stream.of("<response>" + SERVICE_KEY + "&broken;", "<different/>",
                normal.replace("<busiId>ME</busiId>", ""), normal.replace("<statId>28260005</statId>", "<statId />"),
                normal.replace("<lat>37.569620</lat>", "<lat>NaN</lat>"), normal.replace("<lng>126.641973</lng>", "<lng>181</lng>"),
                normal.replace("<lat>37.569620</lat>", "<lat>bad</lat>"),
                normal.replace("<pageNo>1</pageNo>", "<pageNo>2</pageNo>"),
                normal.replace("<numOfRows>10</numOfRows>", "<numOfRows>0</numOfRows>"),
                normal.replace("<totalCount>1</totalCount>", "<totalCount>-1</totalCount>"),
                normal.replace("<resultCode>00</resultCode>", ""),
                readFixture("empty.xml").replace("<totalCount>0</totalCount>", "<totalCount>10</totalCount>"));
    }
    @Test
    @DisplayName("정상 문서 구조여도 외부 entity를 요청하지 않고 거부한다")
    void rejectsExternalEntityWithoutFetching() throws IOException {
        AtomicInteger entityRequests = new AtomicInteger();
        server.createContext("/entity", exchange -> { entityRequests.incrementAndGet(); respond(exchange,200,"외부에서 읽은 이름"); });
        String entityUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/entity";
        String body = readFixture("normal.xml").replace("?>", "?><!DOCTYPE response [<!ENTITY external SYSTEM '" + entityUrl + "'>]>")
                .replace("<statNm>기후대기관</statNm>", "<statNm>&external;</statNm>");
        server.createContext("/getChargerInfo", exchange -> respond(exchange,200,body));
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofSeconds(2)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));

        assertThatThrownBy(() -> client.fetchPage(1)).isInstanceOf(PublicDataException.class).hasMessage("Public data request failed: CONTRACT");
        assertThat(entityRequests.get()).isZero();
    }
    @ParameterizedTest
    @DisplayName("BOM과 XML 선언의 인코딩을 보존해 유효한 XML을 읽는다")
    @MethodSource("encodedXmlResponses")
    void readsDeclaredXmlEncoding(byte[] body) {
        server.createContext("/getChargerInfo", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/xml");
            exchange.sendResponseHeaders(200,body.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(body);
            } finally {
                exchange.close();
            }
        });
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofSeconds(2)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));
        AtomicReference<StationPage> result = new AtomicReference<>();

        assertThatCode(() -> result.set(client.fetchPage(1))).doesNotThrowAnyException();
        assertThat(result.get().snapshots()).singleElement().satisfies(snapshot -> assertThat(snapshot.stationName()).isEqualTo("기후대기관"));
    }
    private static Stream<byte[]> encodedXmlResponses() throws IOException {
        String normal = readFixture("normal.xml");
        return Stream.of(("\uFEFF" + normal).getBytes(StandardCharsets.UTF_8),
                normal.replace("encoding='utf-8'", "encoding='UTF-16'").getBytes(StandardCharsets.UTF_16));
    }
    @Test
    @DisplayName("응답 헤더가 지연되면 timeout으로 종료하고 재시도하지 않는다")
    void timesOutWaitingForHeaders() {
        CountDownLatch responseGate = new CountDownLatch(1);
        AtomicInteger requestCount = new AtomicInteger();
        server.createContext("/getChargerInfo", exchange -> {
            requestCount.incrementAndGet();
            try {
                responseGate.await(5, TimeUnit.SECONDS);
                respond(exchange, 200, "<response/>");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                exchange.close();
            }
        });
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofMillis(500)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));
        long startedAt = System.nanoTime();
        try {
            assertThatThrownBy(() -> client.fetchPage(1)).isInstanceOf(PublicDataException.class)
                    .hasMessage("Public data request failed: TIMEOUT");
            assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofSeconds(3));
            assertThat(requestCount.get()).isEqualTo(1);
        } finally {
            responseGate.countDown();
        }
    }
    @Test
    @DisplayName("헤더 수신 뒤 본문이 멈춰도 전체 응답 timeout으로 종료한다")
    void timesOutWaitingForBody() throws IOException, InterruptedException {
        CountDownLatch bodyGate = new CountDownLatch(1);
        CountDownLatch headersSent = new CountDownLatch(1);
        byte[] body = readFixture("normal.xml").getBytes(StandardCharsets.UTF_8);
        server.createContext("/getChargerInfo", exchange -> {
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(body, 0, 8);
                responseBody.flush();
                headersSent.countDown();
                bodyGate.await(5, TimeUnit.SECONDS);
                responseBody.write(body, 8, body.length - 8);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofMillis(500)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));
        try {
            assertThatThrownBy(() -> client.fetchPage(1)).isInstanceOf(PublicDataException.class)
                    .hasMessage("Public data request failed: TIMEOUT");
            assertThat(headersSent.await(1, TimeUnit.SECONDS)).isTrue();
        } finally {
            bodyGate.countDown();
        }
    }
    @Test
    @DisplayName("연결 실패를 전송 실패로 반환하며 URI·키를 예외에 포함하지 않는다")
    void classifiesConnectionFailure(CapturedOutput output) {
        PublicDataProperties configuration = properties(SERVICE_KEY, Duration.ofSeconds(1));
        server.stop(0);
        client = new DefaultPublicDataClient(configuration, Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));

        assertThatThrownBy(() -> client.fetchPage(1)).isInstanceOf(PublicDataException.class)
                .hasMessage("Public data request failed: TRANSPORT").hasNoCause();
        assertThat(output.getAll()).doesNotContain(SERVICE_KEY);
    }
    @ParameterizedTest
    @DisplayName("0 이하 페이지를 요청하면 네트워크 호출 전에 거부한다")
    @ValueSource(ints = {0, -1})
    void rejectsInvalidPage(int pageNumber) {
        AtomicInteger requestCount = new AtomicInteger();
        server.createContext("/getChargerInfo", exchange -> { requestCount.incrementAndGet(); respond(exchange,200,"<response/>"); });
        client = new DefaultPublicDataClient(properties(SERVICE_KEY, Duration.ofSeconds(2)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));

        assertThatThrownBy(() -> client.fetchPage(pageNumber)).isInstanceOf(IllegalArgumentException.class).hasMessage("page must be at least 1");
        assertThat(requestCount.get()).isZero();
    }
    @ParameterizedTest
    @DisplayName("인증 키가 없으면 네트워크 호출 전에 인증 실패로 반환한다")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void rejectsMissingKey(String serviceKey) {
        AtomicInteger requestCount = new AtomicInteger();
        server.createContext("/getChargerInfo", exchange -> { requestCount.incrementAndGet(); respond(exchange,200,"<response/>"); });
        client = new DefaultPublicDataClient(properties(serviceKey, Duration.ofSeconds(2)), Clock.fixed(COLLECTED_AT, ZoneOffset.UTC));

        assertThatThrownBy(() -> client.fetchPage(1)).isInstanceOf(PublicDataException.class).hasMessage("Public data request failed: AUTHENTICATION");
        assertThat(requestCount.get()).isZero();
    }
    @Test
    @DisplayName("설정 객체의 문자열 표현에 서비스 키를 노출하지 않는다")
    void redactsConfigurationCredentials() {
        assertThat(properties(SERVICE_KEY, Duration.ofSeconds(1)).toString()).doesNotContain(SERVICE_KEY);
    }
    @Test
    @DisplayName("양수가 아닌 응답 timeout 설정을 거부한다")
    void rejectsNonPositiveResponseTimeout() {
        assertThatThrownBy(() -> properties(SERVICE_KEY, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("responseTimeout must be positive");
    }
    @Test
    @DisplayName("양수가 아닌 연결 timeout 설정을 거부한다")
    void rejectsNonPositiveConnectTimeout() {
        assertThatThrownBy(() -> new PublicDataProperties(URI.create("https://example.invalid/getChargerInfo"), SERVICE_KEY, 10, "28", Duration.ZERO, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("connectTimeout must be positive");
    }
    @ParameterizedTest
    @DisplayName("인증정보·query·fragment나 HTTP 주소가 아닌 endpoint 설정을 거부한다")
    @MethodSource("invalidEndpoints")
    void rejectsUnsafeEndpoint(URI endpoint) {
        assertThatThrownBy(() -> new PublicDataProperties(endpoint, SERVICE_KEY, 10, "28", Duration.ofSeconds(1), Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endpoint must be an HTTP URL without credentials, query or fragment");
    }
    private static Stream<URI> invalidEndpoints() {
        return Stream.of(null, URI.create("ftp://example.invalid/path"), URI.create("https:///missing-host"),
                URI.create("https://example.invalid/path?serviceKey=fixture-secret"),
                URI.create("https://fixture-secret@example.invalid/path"), URI.create("https://example.invalid/path#fragment"));
    }
    @ParameterizedTest
    @DisplayName("두 자리 숫자가 아닌 지역 설정을 거부한다")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "1", "280", "ab", "28&serviceKey=leak"})
    void rejectsInvalidRegion(String region) {
        assertThatThrownBy(() -> new PublicDataProperties(URI.create("https://example.invalid/path"), SERVICE_KEY, 10, region, Duration.ofSeconds(1), Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("region must be a two-digit code");
    }
    @ParameterizedTest
    @DisplayName("공식 허용 범위를 벗어난 페이지 크기 설정을 거부한다")
    @ValueSource(ints = {0, 9, 10000})
    void rejectsInvalidPageSize(int pageSize) {
        assertThatThrownBy(() -> new PublicDataProperties(URI.create("https://example.invalid/getChargerInfo"), SERVICE_KEY, pageSize, "28", Duration.ofSeconds(1), Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("pageSize must be between 10 and 9999");
    }
}
