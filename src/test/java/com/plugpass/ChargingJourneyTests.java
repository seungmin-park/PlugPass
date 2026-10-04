package com.plugpass;

import com.plugpass.ingestion.client.DefaultPublicDataClient;
import com.plugpass.ingestion.client.PublicDataClient;
import com.plugpass.ingestion.config.PublicDataProperties;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.dto.StationPage;
import com.plugpass.ingestion.dto.StationSnapshot;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.ingestion.service.StationSyncService;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.repository.StationRepository;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "plugpass.ingestion.retry-delay=PT0S")
@Import(ChargingJourneyTests.ExternalBoundary.class)
class ChargingJourneyTests {
    private static final String QUERY = "?latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO";
    @LocalServerPort private int port;
    @Autowired private StationSyncService stationSyncService;
    @Autowired private StationRepository stationRepository;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private SyncRunRepository syncRunRepository;
    @Autowired private JourneyProvider journeyProvider;
    @Autowired private JourneyClock journeyClock;
    @Autowired private JsonMapper jsonMapper;

    @AfterEach
    void deleteOwnedState() {
        chargerRepository.deleteAllInBatch();
        stationRepository.deleteAllInBatch();
        syncRunRepository.deleteAllInBatch();
        journeyProvider.status.set(200);
        journeyProvider.xml = "<response/>";
        journeyProvider.observedAt = null;
        journeyClock.now = Instant.parse("2026-10-05T00:00:00Z");
    }

    @Test
    @DisplayName("실제 HTTP 수집·중복 방지·주변 검색·상세·첫 충전소 제외 후 대체 후보를 연결한다")
    void findsAnAlternativeWithoutInventingSourceFreshness() throws Exception {
        journeyProvider.xml = response(item("first", "가까운 충전소", "127.000", "2")
                + item("second", "대체 충전소", "127.001", "2"), 2);
        assertThat(stationSyncService.synchronize().status()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(stationSyncService.synchronize().status()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(stationRepository.count()).isEqualTo(2);
        assertThat(chargerRepository.count()).isEqualTo(2);
        try (HttpClient client = HttpClient.newHttpClient()) {
            JsonNode search = get(client, "/api/v1/stations" + QUERY, "search");
            assertThat(search.path("dataReady").asBoolean()).isTrue();
            assertThat(search.path("lastSuccessfulRunAt").asString()).isEqualTo(journeyClock.instant().toString());
            assertThat(search.path("stations").size()).isEqualTo(2);
            JsonNode first = search.path("stations").get(0);
            assertThat(first.path("name").asString()).isEqualTo("가까운 충전소");
            assertThat(first.path("distanceMeters").asDouble()).isZero();
            assertThat(first.path("compatibleChargerCount").asInt()).isEqualTo(1);
            assertThat(first.path("reportedAvailableCount").asInt()).isEqualTo(1);
            long firstId = first.path("id").asLong();
            long secondId = search.path("stations").get(1).path("id").asLong();
            JsonNode detail = get(client, "/api/v1/stations/" + firstId, "detail");
            assertThat(detail.path("providerStationId").asString()).isEqualTo("first");
            assertThat(detail.path("chargers").size()).isEqualTo(1);
            JsonNode charger = detail.path("chargers").get(0);
            assertThat(charger.path("chargerId").asString()).isEqualTo("01");
            assertThat(charger.path("status").asString()).isEqualTo("AVAILABLE");
            assertThat(charger.path("sourceObservedAt").isNull()).isTrue();
            assertThat(charger.path("freshness").asString()).isEqualTo("UNVERIFIED");
            assertThat(charger.path("reasonCode").asString()).isEqualTo("SOURCE_OBSERVED_AT_MISSING");
            assertThat(charger.path("collectedAt").asString()).isEqualTo(journeyClock.instant().toString());
            JsonNode alternative = get(client, "/api/v1/recommendations" + QUERY + "&excludeStationId=" + firstId, "alternative");
            assertThat(alternative.path("preferred").size()).isZero();
            assertThat(alternative.path("excluded").size()).isZero();
            assertThat(alternative.path("requiresConfirmation").size()).isEqualTo(1);
            assertThat(alternative.path("requiresConfirmation").get(0).path("id").asLong()).isEqualTo(secondId);
            assertThat(alternative.path("requiresConfirmation").get(0).path("reasonCodes").toString())
                    .isEqualTo("[\"UNVERIFIED_AVAILABLE\",\"SOURCE_OBSERVED_AT_MISSING\"]");
        }
    }

    @Test
    @DisplayName("합성 관측 시각의 시간 경과·외부503·복구를 실제 HTTP로 확인하고 기존 저장 값을 보호한다")
    void agesAndRecoversTrustedSyntheticObservations() throws Exception {
        journeyProvider.xml = response(item("trusted", "합성 관측 충전소", "127", "2"), 1);
        journeyProvider.observedAt = journeyClock.instant();
        assertThat(stationSyncService.synchronize().status()).isEqualTo(SyncStatus.SUCCESS);
        long stationId = stationRepository.findByProviderAndStationId("ME", "trusted").orElseThrow().getDatabaseId();
        try (HttpClient client = HttpClient.newHttpClient()) {
            JsonNode recent = get(client, "/api/v1/recommendations" + QUERY, "recent");
            assertThat(recent.path("preferred").size()).isEqualTo(1);
            assertThat(recent.path("preferred").get(0).path("reasonCodes").toString()).isEqualTo("[\"RECENT_AVAILABLE\"]");
            journeyClock.now = journeyClock.now.plusSeconds(601);
            JsonNode stale = get(client, "/api/v1/recommendations" + QUERY, "stale");
            assertThat(stale.path("preferred").size()).isZero();
            assertThat(stale.path("requiresConfirmation").size()).isZero();
            assertThat(stale.path("excluded").size()).isEqualTo(1);
            assertThat(stale.path("excluded").get(0).path("id").asLong()).isEqualTo(stationId);
            assertThat(stale.path("excluded").get(0).path("reasonCodes").toString()).isEqualTo("[\"MAX_AGE_EXCEEDED\"]");
            journeyProvider.status.set(503);
            assertThat(stationSyncService.synchronize().failureCode()).isEqualTo("SERVER");
            JsonNode failure = get(client, "/api/v1/stations/" + stationId, "failure");
            JsonNode saved = failure.path("chargers").get(0);
            assertThat(saved.path("status").asString()).isEqualTo("AVAILABLE");
            assertThat(saved.path("freshness").asString()).isEqualTo("STALE");
            assertThat(saved.path("collectedAt").asString()).isEqualTo("2026-10-05T00:00:00Z");
            JsonNode duringFailure = get(client, "/api/v1/stations" + QUERY, "search-during-failure");
            assertThat(duringFailure.path("lastSuccessfulRunAt").asString()).isEqualTo("2026-10-05T00:00:00Z");
            assertThat(get(client, "/api/v1/recommendations" + QUERY, "recommendations-during-failure")).isEqualTo(stale);
            journeyProvider.status.set(200);
            journeyProvider.observedAt = journeyClock.instant();
            assertThat(stationSyncService.synchronize().status()).isEqualTo(SyncStatus.SUCCESS);
            JsonNode recovered = get(client, "/api/v1/recommendations" + QUERY, "recovered");
            assertThat(recovered).isEqualTo(recent);
            JsonNode updated = get(client, "/api/v1/stations/" + stationId, "updated");
            assertThat(updated.path("chargers").get(0).path("collectedAt").asString()).isEqualTo(journeyClock.instant().toString());
        }
        assertThat(stationRepository.count()).isEqualTo(1);
        assertThat(chargerRepository.count()).isEqualTo(1);
        assertThat(syncRunRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("수집이 성공해도 반경 밖이면 검색·추천은 빈 결과를 반환한다")
    void returnsNoCandidatesOutsideTheRadius() throws Exception {
        journeyProvider.xml = response(item("far", "반경 밖", "128", "2"), 1);
        assertThat(stationSyncService.synchronize().status()).isEqualTo(SyncStatus.SUCCESS);
        try (HttpClient client = HttpClient.newHttpClient()) {
            assertThat(get(client, "/api/v1/stations" + QUERY, "empty-search").path("stations").size()).isZero();
            JsonNode empty = get(client, "/api/v1/recommendations" + QUERY, "empty");
            assertThat(empty.toString()).isEqualTo("{\"preferred\":[],\"requiresConfirmation\":[],\"excluded\":[]}");
        }
    }

    private JsonNode get(HttpClient client, String path, String evidenceName) throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        Path directory = Path.of("build/verification/charging-journey");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(evidenceName + ".json"), response.body());
        return jsonMapper.readTree(response.body());
    }
    private String item(String stationId, String name, String longitude, String status) {
        return "<item><busiId>ME</busiId><statId>" + stationId + "</statId><chgerId>01</chgerId><statNm>" + name
                + "</statNm><lat>37.5</lat><lng>" + longitude + "</lng><stat>" + status
                + "</stat><chgerType>04</chgerType><useTime>24시간</useTime><limitYn>N</limitYn><delYn>N</delYn></item>";
    }
    private String response(String items, int count) {
        return "<response><header><resultCode>00</resultCode><pageNo>1</pageNo><numOfRows>10</numOfRows><totalCount>"
                + count + "</totalCount></header><body><items>" + items + "</items></body></response>";
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class ExternalBoundary {
        @Bean @Primary JourneyClock journeyClock() { return new JourneyClock(); }
        @Bean(destroyMethod = "close") @Primary JourneyProvider journeyProvider(JourneyClock clock) throws IOException {
            return new JourneyProvider(clock);
        }
    }
    static class JourneyClock extends Clock {
        volatile Instant now = Instant.parse("2026-10-05T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        public Instant instant() { return now; }
    }
    static class JourneyProvider implements PublicDataClient {
        final HttpServer server;
        final DefaultPublicDataClient delegate;
        final AtomicInteger status = new AtomicInteger(200);
        volatile String xml = "<response/>";
        volatile Instant observedAt;
        JourneyProvider(Clock clock) throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/fixture", exchange -> {
                byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status.get(), bytes.length);
                try (exchange) { exchange.getResponseBody().write(bytes); }
            });
            server.start();
            delegate = new DefaultPublicDataClient(new PublicDataProperties(
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/fixture"), "fixture-only", 10, "28",
                    Duration.ofSeconds(1), Duration.ofSeconds(2)), clock);
        }
        public StationPage fetchPage(int page) { return fetchPage(page, Duration.ofSeconds(2)); }
        public StationPage fetchPage(int page, Duration remaining) {
            StationPage response = delegate.fetchPage(page, remaining);
            return new StationPage(response.pageNumber(), response.pageSize(), response.totalCount(), response.snapshots().stream()
                    .map(snapshot -> new StationSnapshot(snapshot.chargerId(), snapshot.stationName(), snapshot.location(), snapshot.status(),
                            snapshot.rawStatus(), snapshot.details(), observedAt, snapshot.collectedAt())).toList());
        }
        public void close() { delegate.close(); server.stop(0); }
    }
}
