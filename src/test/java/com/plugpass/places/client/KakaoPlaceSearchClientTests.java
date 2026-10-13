package com.plugpass.places.client;

import com.plugpass.exception.PlaceSearchException;
import com.plugpass.places.config.KakaoLocalProperties;
import com.plugpass.places.dto.PlaceMatch;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoPlaceSearchClientTests {
    private HttpServer server;
    private KakaoPlaceSearchClient client;
    private final JsonMapper mapper = JsonMapper.builder().build();
    @BeforeEach
    void openTransport() throws Exception { server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); server.start(); }
    @AfterEach
    void closeTransport() { if (client != null) client.close(); server.stop(0); }

    @Test
    @DisplayName("주소와 장소를 실제 HTTP로 조회하여 좌표를 변환하고 중복을 합친다")
    void combinesAddressAndKeyword() {
        List<String> requests = new CopyOnWriteArrayList<>();
        server.createContext("/v2/local/search/address.json", exchange -> {
            requests.add(exchange.getRequestURI().getRawQuery());
            requests.add(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = mapper.writeValueAsBytes(Map.of("documents", List.of(Map.of("address_name", "서울 주소", "x", "127", "y", "37.5"))));
            exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.createContext("/v2/local/search/keyword.json", exchange -> {
            requests.add(exchange.getRequestURI().getRawQuery());
            byte[] body = mapper.writeValueAsBytes(Map.of("documents", List.of(
                Map.of("place_name", "같은 위치", "road_address_name", "서울 주소", "address_name", "지번", "x", "127", "y", "37.5"),
                Map.of("place_name", "서울 시청", "road_address_name", "", "address_name", "서울 다른 주소", "x", "127.1", "y", "37.6"))));
            exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        client = new KakaoPlaceSearchClient(properties("fixture-key", Duration.ofSeconds(1)), mapper);
        assertThat(client.search("서울 & 시청")).containsExactly(new PlaceMatch("서울 주소", "서울 주소", 37.5, 127), new PlaceMatch("서울 시청", "서울 다른 주소", 37.6, 127.1));
        assertThat(requests).containsExactly("query=%EC%84%9C%EC%9A%B8+%26+%EC%8B%9C%EC%B2%AD&size=10", "KakaoAK fixture-key", "query=%EC%84%9C%EC%9A%B8+%26+%EC%8B%9C%EC%B2%AD&size=10");
    }
    @Test
    @DisplayName("주소와 장소가 모두 비어 있으면 빈 결과를 반환한다")
    void acceptsEmptyResults() {
        respond(200, mapper.writeValueAsString(Map.of("documents", List.of())));
        client = new KakaoPlaceSearchClient(properties("fixture-key", Duration.ofSeconds(1)), mapper);
        assertThat(client.search("없는 장소")).isEmpty();
    }
    @ParameterizedTest
    @DisplayName("외부 HTTP 실패를 구분하고 비밀값과 원문을 노출하지 않는다")
    @CsvSource({"401,AUTHENTICATION", "403,AUTHENTICATION", "429,RATE_LIMIT", "500,UPSTREAM", "302,UPSTREAM"})
    void classifiesHttpFailure(int status, PlaceSearchException.Failure failure) {
        respond(status, "fixture-key private error");
        client = new KakaoPlaceSearchClient(properties("fixture-key", Duration.ofSeconds(1)), mapper);
        assertThatThrownBy(() -> client.search("서울")).isInstanceOfSatisfying(PlaceSearchException.class,
                exception -> { assertThat(exception.failure()).isEqualTo(failure); assertThat(exception.getMessage()).doesNotContain("fixture-key", "private error"); });
    }
    @ParameterizedTest
    @DisplayName("깨진 JSON과 누락되거나 잘못된 좌표를 빈 결과로 숨기지 않는다")
    @ValueSource(strings = {"{broken", "{}", "{\"documents\":[{\"address_name\":\"서울\",\"x\":\"NaN\",\"y\":\"37\"}]}", "{\"documents\":[{\"address_name\":\"서울\",\"x\":\"127\",\"y\":\"91\"}]}"})
    void rejectsMalformedResponse(String body) {
        respond(200, body);
        client = new KakaoPlaceSearchClient(properties("fixture-key", Duration.ofSeconds(1)), mapper);
        assertThatThrownBy(() -> client.search("서울")).isInstanceOfSatisfying(PlaceSearchException.class,
                exception -> assertThat(exception.failure()).isEqualTo(PlaceSearchException.Failure.INVALID_RESPONSE));
    }
    @Test
    @DisplayName("응답 기한을 넘긴 요청은 자동 재시도 없이 종료한다")
    void timesOut() {
        List<String> requests = new CopyOnWriteArrayList<>();
        server.createContext("/", exchange -> { requests.add(exchange.getRequestURI().getPath()); });
        client = new KakaoPlaceSearchClient(properties("fixture-key", Duration.ofMillis(150)), mapper);
        assertThatThrownBy(() -> client.search("서울")).isInstanceOfSatisfying(PlaceSearchException.class,
                exception -> assertThat(exception.failure()).isEqualTo(PlaceSearchException.Failure.TIMEOUT));
        assertThat(requests).hasSize(1);
    }
    private void respond(int status, String body) {
        server.createContext("/", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
    }
    private KakaoLocalProperties properties(String key, Duration timeout) {
        return new KakaoLocalProperties(URI.create("http://127.0.0.1:" + server.getAddress().getPort()), key, timeout);
    }
}
