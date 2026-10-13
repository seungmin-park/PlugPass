package com.plugpass.places.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "plugpass.kakao-local.rest-api-key=")
class PlaceSearchFlowTests {
    @LocalServerPort private int port;
    @Test
    @DisplayName("키 없이 주소 검색하면 설정 안내를 반환하고 충전소 검색은 계속 제공한다")
    void reportsUnavailableWithoutBlockingStations() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> places = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/places?query=Seoul")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(places.statusCode()).isEqualTo(503);
            assertThat(places.body()).contains("PLACE_SEARCH_NOT_CONFIGURED", "주소 검색 준비 중입니다");
            HttpResponse<String> stations = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/stations?latitude=37.5&longitude=127&radiusMeters=1000&connector=DC_COMBO")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(stations.statusCode()).isEqualTo(200);
        }
    }
}
