package com.plugpass.ingestion.client;

import com.plugpass.ingestion.config.PublicDataProperties;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IngestionHttpBudgetTests {
    @Test
    @DisplayName("본문이 멈춘 실제 HTTP도 남은 회차 시간 안에서 timeout으로 종료한다")
    void limitsBodyToRemainingBudget() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        CountDownLatch release = new CountDownLatch(1);
        server.createContext("/fixture",exchange -> {
            byte[] body = "<response/>".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,body.length);
            try (exchange) {
                try { release.await(250,TimeUnit.MILLISECONDS); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                exchange.getResponseBody().write(body);
            }
        });
        server.start();
        try (PublicDataClient client = client(server)) {
            long started = System.nanoTime();
            assertThatThrownBy(() -> client.fetchPage(1,Duration.ofMillis(50))).isInstanceOfSatisfying(PublicDataException.class,
                    failure -> assertThat(failure.getFailure()).isEqualTo(PublicDataFailure.TIMEOUT));
            assertThat(Duration.ofNanos(System.nanoTime()-started)).isLessThan(Duration.ofSeconds(2));
        } finally { release.countDown(); server.stop(0); }
    }
    @ParameterizedTest
    @DisplayName("실제 HTTP Retry-After의 초·날짜를 해석하고 잘못된 값은 버린다")
    @MethodSource("retryHeaders")
    void parsesRetryAfter(String header, Duration expected) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/fixture",exchange -> {
            exchange.getResponseHeaders().set("Retry-After",header);
            exchange.sendResponseHeaders(429,-1); exchange.close();
        });
        server.start();
        try (PublicDataClient client = client(server)) {
            assertThatThrownBy(() -> client.fetchPage(1)).isInstanceOfSatisfying(PublicDataException.class,failure -> {
                assertThat(failure.getFailure()).isEqualTo(PublicDataFailure.RATE_LIMIT);
                assertThat(failure.getRetryAfter()).isEqualTo(expected);
                assertThat(failure.getMessage()).doesNotContain("fixture-secret",header,"http://");
            });
        } finally { server.stop(0); }
    }
    static Stream<Arguments> retryHeaders() {
        return Stream.of(Arguments.of("2",Duration.ofSeconds(2)),Arguments.of("Mon, 05 Oct 2026 00:00:03 GMT",Duration.ofSeconds(3)),
                Arguments.of("not-a-date",null),Arguments.of("-1",null),Arguments.of("999999999999999999999999999999",null));
    }
    private PublicDataClient client(HttpServer server) {
        PublicDataProperties properties = new PublicDataProperties(URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/fixture"),
                "fixture-secret",10,"28",Duration.ofSeconds(1),Duration.ofSeconds(2));
        return new DefaultPublicDataClient(properties,Clock.fixed(Instant.parse("2026-10-05T00:00:00Z"),ZoneOffset.UTC));
    }
}
