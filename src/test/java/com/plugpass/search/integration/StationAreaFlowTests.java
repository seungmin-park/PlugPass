package com.plugpass.search.integration;

import com.plugpass.ingestion.dto.StationSnapshot;
import com.plugpass.ingestion.service.StationUpsertService;
import com.plugpass.station.domain.ChargerDetails;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.domain.GeoPoint;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.repository.StationRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StationAreaFlowTests {
    @LocalServerPort private int port;
    @Autowired private StationUpsertService stationUpsertService;
    @Autowired private StationRepository stationRepository;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private JsonMapper jsonMapper;
    @AfterEach
    void cleanOwnedData() { chargerRepository.deleteAllInBatch(); stationRepository.deleteAllInBatch(); }

    @Test
    @DisplayName("지도 밖의 더 가까운 충전소를 제외한 뒤 개수 제한을 적용한다")
    void filtersAreaBeforeLimit() throws Exception {
        saveStation("outside", new GeoPoint(37.50051,127), "04");
        saveStation("inside", new GeoPoint(37.5,127.003), "04");
        saveStation("incompatible", new GeoPoint(37.5,127), "02");
        HttpResponse<String> response = request("south=37.4995&north=37.5005&west=126.995&east=127.005&connector=DC_COMBO&limit=1");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = jsonMapper.readTree(response.body());
        assertThat(body.path("stations").size()).isEqualTo(1);
        assertThat(body.path("stations").get(0).path("providerStationId").asString()).isEqualTo("inside");
        assertThat(body.path("stations").get(0).path("compatibleChargerCount").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("지도 모서리는 포함하고 바로 밖은 제외한다")
    void includesAreaBoundary() throws Exception {
        saveStation("boundary", new GeoPoint(37.501,127.001), "04");
        saveStation("outside", new GeoPoint(37.5010001,127.001), "04");
        HttpResponse<String> response = request("south=37.499&north=37.501&west=126.999&east=127.001&connector=DC_COMBO");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode stations = jsonMapper.readTree(response.body()).path("stations");
        assertThat(stations.size()).isEqualTo(1);
        assertThat(stations.get(0).path("providerStationId").asString()).isEqualTo("boundary");
    }

    @Test
    @DisplayName("영역 검색의 빈 결과도 데이터 준비 여부와 함께 반환한다")
    void returnsEmptyArea() throws Exception {
        HttpResponse<String> response = request("south=37.499&north=37.501&west=126.999&east=127.001&connector=DC_COMBO");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = jsonMapper.readTree(response.body());
        assertThat(body.path("dataReady").asBoolean()).isFalse();
        assertThat(body.path("lastSuccessfulRunAt").isNull()).isTrue();
        assertThat(body.path("stations").isArray()).isTrue();
        assertThat(body.path("stations").size()).isZero();
    }

    @Test
    @DisplayName("같은 거리는 내부 ID 순서로 안정 정렬하고 limit을 적용한다")
    void ordersTiesById() throws Exception {
        saveStation("first", new GeoPoint(37.5,127), "04");
        saveStation("second", new GeoPoint(37.5,127), "04");
        saveStation("farther", new GeoPoint(37.5005,127), "04");
        HttpResponse<String> response = request("south=37.499&north=37.501&west=126.999&east=127.001&connector=DC_COMBO&limit=2");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode stations = jsonMapper.readTree(response.body()).path("stations");
        assertThat(stations.size()).isEqualTo(2);
        assertThat(stations.get(0).path("providerStationId").asString()).isEqualTo("first");
        assertThat(stations.get(1).path("providerStationId").asString()).isEqualTo("second");
    }

    private void saveStation(String stationId, GeoPoint location, String connectorCode) {
        Instant now = Instant.parse("2026-10-13T00:00:00Z");
        ChargerDetails details = new ChargerDetails(connectorCode,"24시간","N",null,null,null,null,null,null);
        stationUpsertService.upsert(new StationSnapshot(new ChargerId("ME",stationId,"01"),stationId,location,
                ChargerStatus.AVAILABLE,"2",details,now,now));
    }
    private HttpResponse<String> request(String query) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/stations/area?"+query)).GET().build(),HttpResponse.BodyHandlers.ofString());
        }
    }
}
