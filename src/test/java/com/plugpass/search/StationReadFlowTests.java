package com.plugpass.search;

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
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.HttpServer;
import com.plugpass.ingestion.DefaultPublicDataClient;
import com.plugpass.ingestion.PublicDataClient;
import com.plugpass.ingestion.PublicDataProperties;
import com.plugpass.ingestion.StationSyncService;
import com.plugpass.ingestion.SyncResult;
import com.plugpass.ingestion.SyncStatus;
import com.plugpass.ingestion.SyncRunRepository;
import com.plugpass.station.ChargerRepository;
import com.plugpass.station.StationRepository;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(StationReadFlowTests.ExternalBoundary.class)
class StationReadFlowTests {
    @LocalServerPort private int port;
    @Autowired private StationSyncService stationSyncService;
    @Autowired private StationRepository stationRepository;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private SyncRunRepository syncRunRepository;
    @Autowired private FixtureProvider fixtureProvider;
    @Autowired private JsonMapper jsonMapper;
    @AfterEach
    void cleanOwnedState() {
        chargerRepository.deleteAllInBatch(); stationRepository.deleteAllInBatch(); syncRunRepository.deleteAllInBatch();
        fixtureProvider.status.set(200); fixtureProvider.requests.set(0); fixtureProvider.xml.set("<response/>");
    }
    @Test
    @DisplayName("실제 fixture HTTP 수집부터 H2·검색·상세 HTTP까지 연결하고 재수집 중복을 막는다")
    void connectsCollectionToReadApis() throws Exception {
        fixtureProvider.xml.set(Files.readString(Path.of("src/test/resources/publicdata/normal.xml")));
        SyncResult first = stationSyncService.synchronize();
        SyncResult second = stationSyncService.synchronize();
        assertThat(first.status()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(second.status()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(stationRepository.count()).isEqualTo(1);
        assertThat(chargerRepository.count()).isEqualTo(1);
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpResponse<String> searchResponse = get(httpClient,"/api/v1/stations?latitude=37.569620&longitude=126.641973&radiusMeters=1000&connector=DC_CHADEMO");
            assertThat(searchResponse.statusCode()).isEqualTo(200);
            JsonNode search = jsonMapper.readTree(searchResponse.body());
            assertThat(search.path("dataReady").asBoolean()).isTrue();
            assertThat(search.path("lastSuccessfulRunAt").asString()).isEqualTo("2026-10-02T00:00:00Z");
            assertThat(search.path("stations").size()).isEqualTo(1);
            assertThat(search.path("stations").get(0).path("name").asString()).isEqualTo("기후대기관");
            long stationId = search.path("stations").get(0).path("id").asLong();
            HttpResponse<String> detailResponse = get(httpClient,"/api/v1/stations/"+stationId);
            assertThat(detailResponse.statusCode()).isEqualTo(200);
            JsonNode detail = jsonMapper.readTree(detailResponse.body());
            assertThat(detail.path("providerStationId").asString()).isEqualTo("28260005");
            assertThat(detail.path("chargers").size()).isEqualTo(1);
            JsonNode charger = detail.path("chargers").get(0);
            assertThat(charger.path("chargerId").asString()).isEqualTo("02");
            assertThat(charger.path("status").asString()).isEqualTo("AVAILABLE");
            assertThat(charger.path("freshness").asString()).isEqualTo("UNVERIFIED");
            assertThat(charger.path("reasonCode").asString()).isEqualTo("SOURCE_OBSERVED_AT_MISSING");
            assertThat(charger.path("sourceObservedAt").isNull()).isTrue();
            assertThat(charger.path("collectedAt").asString()).isEqualTo("2026-10-02T00:00:00Z");
            assertThat(charger.path("sourceStatusChangedAtRaw").asString()).isEqualTo("20190829121020");
            assertThat(charger.path("note").asString()).isEqualTo("공사로 인해 이용 불가");
            assertThat(charger.path("limitYn").asString()).isEqualTo("N");
            HttpResponse<String> recommendationResponse = get(httpClient,"/api/v1/recommendations?latitude=37.569620&longitude=126.641973&radiusMeters=1000&connector=DC_CHADEMO");
            assertThat(recommendationResponse.statusCode()).isEqualTo(200);
            JsonNode recommendation = jsonMapper.readTree(recommendationResponse.body());
            assertThat(recommendation.path("preferred").size()).isZero();
            assertThat(recommendation.path("requiresConfirmation").size()).isEqualTo(1);
            assertThat(recommendation.path("requiresConfirmation").get(0).path("id").asLong()).isEqualTo(stationId);
            assertThat(recommendation.path("requiresConfirmation").get(0).path("reasonCodes").get(0).asString()).isEqualTo("UNVERIFIED_AVAILABLE");
            Path evidence = Path.of("build/verification/station-read-flow");
            Files.createDirectories(evidence);
            Files.writeString(evidence.resolve("recommendations.json"),recommendationResponse.body());
            Files.writeString(evidence.resolve("search.json"),searchResponse.body());
            Files.writeString(evidence.resolve("detail.json"),detailResponse.body());
        }
        assertThat(fixtureProvider.requests).hasValue(2);
    }
    @Test
    @DisplayName("공급자 HTTP 장애 후에도 마지막 성공 시각과 저장 데이터로 검색·상세를 제공한다")
    void readsDuringProviderFailure() throws Exception {
        fixtureProvider.xml.set(Files.readString(Path.of("src/test/resources/publicdata/normal.xml")));
        stationSyncService.synchronize();
        Long stationId = stationRepository.findByProviderAndStationId("ME","28260005").orElseThrow().getDatabaseId();
        fixtureProvider.status.set(503);
        SyncResult failure = stationSyncService.synchronize();
        assertThat(failure.status()).isEqualTo(SyncStatus.FAILURE);
        assertThat(failure.failureCode()).isEqualTo("SERVER");
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpResponse<String> searchResponse = get(httpClient,"/api/v1/stations?latitude=37.569620&longitude=126.641973&radiusMeters=1000&connector=DC_CHADEMO");
            assertThat(searchResponse.statusCode()).isEqualTo(200);
            JsonNode search = jsonMapper.readTree(searchResponse.body());
            assertThat(search.path("lastSuccessfulRunAt").asString()).isEqualTo("2026-10-02T00:00:00Z");
            assertThat(search.path("stations").size()).isEqualTo(1);
            HttpResponse<String> detailResponse = get(httpClient,"/api/v1/stations/"+stationId);
            assertThat(detailResponse.statusCode()).isEqualTo(200);
            assertThat(jsonMapper.readTree(detailResponse.body()).path("chargers").get(0).path("freshness").asString()).isEqualTo("UNVERIFIED");
            HttpResponse<String> recommendationResponse = get(httpClient,"/api/v1/recommendations?latitude=37.569620&longitude=126.641973&radiusMeters=1000&connector=DC_CHADEMO");
            assertThat(recommendationResponse.statusCode()).isEqualTo(200);
            JsonNode recommendation = jsonMapper.readTree(recommendationResponse.body());
            assertThat(recommendation.path("preferred").size()).isZero();
            assertThat(recommendation.path("requiresConfirmation").size()).isEqualTo(1);
            assertThat(recommendation.path("requiresConfirmation").get(0).path("id").asLong()).isEqualTo(stationId);
            assertThat(recommendation.path("requiresConfirmation").get(0).path("reasonCodes").get(0).asString()).isEqualTo("UNVERIFIED_AVAILABLE");
            Path evidence = Path.of("build/verification/station-read-flow");
            Files.createDirectories(evidence);
            Files.writeString(evidence.resolve("recommendations.json"),recommendationResponse.body());
            Files.writeString(evidence.resolve("search-during-provider-failure.json"),searchResponse.body());
            Files.writeString(evidence.resolve("detail-during-provider-failure.json"),detailResponse.body());
        }
        assertThat(fixtureProvider.requests).hasValue(2);
        assertThat(chargerRepository.count()).isEqualTo(1);
    }
    private HttpResponse<String> get(HttpClient httpClient, String path) throws Exception {
        return httpClient.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(5)).GET().build(),HttpResponse.BodyHandlers.ofString());
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class ExternalBoundary {
        @Bean(destroyMethod = "close") FixtureProvider fixtureProvider() throws IOException { return new FixtureProvider(); }
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-10-02T00:00:00Z"),ZoneOffset.UTC); }
        @Bean(destroyMethod = "close") @Primary PublicDataClient fixtureClient(FixtureProvider fixtureProvider, Clock clock) {
            PublicDataProperties properties = new PublicDataProperties(fixtureProvider.endpoint(),"fixture-only",10,"28",Duration.ofSeconds(1),Duration.ofSeconds(2));
            return new DefaultPublicDataClient(properties,clock);
        }
    }
    static class FixtureProvider implements AutoCloseable {
        final AtomicReference<String> xml = new AtomicReference<>("<response/>");
        final AtomicInteger status = new AtomicInteger(200);
        final AtomicInteger requests = new AtomicInteger();
        final HttpServer server;
        FixtureProvider() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/fixture",exchange -> {
                requests.incrementAndGet();
                byte[] body = xml.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type","application/xml; charset=UTF-8");
                exchange.sendResponseHeaders(status.get(),body.length);
                try (exchange) { exchange.getResponseBody().write(body); }
            });
            server.start();
        }
        URI endpoint() { return URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/fixture"); }
        public void close() { server.stop(0); }
    }
}
