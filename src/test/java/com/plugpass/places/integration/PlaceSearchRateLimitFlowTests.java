package com.plugpass.places.integration;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"plugpass.kakao-local.rest-api-key=fixture-key", "plugpass.kakao-local.max-searches-per-minute=1"})
class PlaceSearchRateLimitFlowTests {
    private static final AtomicInteger upstreamRequests = new AtomicInteger();
    private static final HttpServer kakao = startKakaoStub();
    @LocalServerPort private int port;

    @DynamicPropertySource
    static void pointPlaceSearchAtLoopbackStub(DynamicPropertyRegistry registry) {
        registry.add("plugpass.kakao-local.base-url", () -> "http://127.0.0.1:" + kakao.getAddress().getPort());
    }

    @AfterAll
    static void stopKakaoStub() { kakao.stop(0); }

    @Test
    @DisplayName("분당 주소 검색 한도를 넘은 익명 요청은 카카오를 호출하지 않고 요청 과다 오류를 반환한다")
    void rejectsSearchesBeyondBudgetWithoutUpstreamCall() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> first = client.send(placeRequest(), HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> second = client.send(placeRequest(), HttpResponse.BodyHandlers.ofString());

            assertThat(first.statusCode()).isEqualTo(200);
            assertThat(second.statusCode()).isEqualTo(429);
            assertThat(second.body()).contains("PLACE_SEARCH_RATE_LIMIT", "주소 검색 요청이 많습니다");
            assertThat(upstreamRequests.get()).isEqualTo(2);
        }
    }

    private HttpRequest placeRequest() {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/places?query=Seoul")).build();
    }

    private static HttpServer startKakaoStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v2/local/search/", exchange -> {
                upstreamRequests.incrementAndGet();
                byte[] body = "{\"documents\":[]}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
