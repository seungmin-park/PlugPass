package e2e;

import com.plugpass.PlugPassApplication;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.repository.StationRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { PlugPassApplication.class, ChargingDemoApplication.DemoConfiguration.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChargingDemoApplicationTests {
    private static final String QUERY = "?latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO&limit=20";
    @LocalServerPort private int port;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private StationRepository stationRepository;
    @Autowired private SyncRunRepository syncRunRepository;
    @Autowired private JsonMapper jsonMapper;

    @AfterEach
    void deleteOwnedData() {
        chargerRepository.deleteAllInBatch();
        stationRepository.deleteAllInBatch();
        syncRunRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("브라우저 데모는 실제 HTTP 공급자·수집·H2·상세·제외 추천을 연결한다")
    void loadsTwoSyntheticStationsThroughRealIngestion() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            JsonNode search = get(client, "/api/v1/stations" + QUERY);
            assertThat(search.path("stations").size()).isEqualTo(2);
            assertThat(search.path("dataReady").asBoolean()).isTrue();
            assertThat(search.path("lastSuccessfulRunAt").asString()).isEqualTo("2026-10-05T00:00:00Z");
            long firstId = search.path("stations").get(0).path("id").asLong();
            long secondId = search.path("stations").get(1).path("id").asLong();
            assertThat(search.path("stations").get(0).path("name").asString()).isEqualTo("데모 가까운 충전소");
            JsonNode detail = get(client, "/api/v1/stations/" + firstId);
            assertThat(detail.path("providerStationId").asString()).isEqualTo("browser_first");
            assertThat(detail.path("chargers").get(0).path("sourceObservedAt").isNull()).isTrue();
            assertThat(detail.path("chargers").get(0).path("freshness").asString()).isEqualTo("UNVERIFIED");
            JsonNode candidates = get(client, "/api/v1/recommendations" + QUERY + "&excludeStationId=" + firstId);
            assertThat(candidates.path("preferred").size()).isZero();
            assertThat(candidates.path("excluded").size()).isZero();
            assertThat(candidates.path("requiresConfirmation").size()).isEqualTo(1);
            assertThat(candidates.path("requiresConfirmation").get(0).path("id").asLong()).isEqualTo(secondId);
            assertThat(candidates.path("requiresConfirmation").get(0).path("reasonCodes").toString())
                    .isEqualTo("[\"UNVERIFIED_AVAILABLE\",\"SOURCE_OBSERVED_AT_MISSING\"]");
            JsonNode alternativeDetail = get(client, "/api/v1/stations/" + secondId);
            assertThat(alternativeDetail.path("providerStationId").asString()).isEqualTo("browser_second");
            assertThat(stationRepository.count()).isEqualTo(2);
            assertThat(chargerRepository.count()).isEqualTo(2);
        }
    }

    private JsonNode get(HttpClient client, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return jsonMapper.readTree(response.body());
    }
}
