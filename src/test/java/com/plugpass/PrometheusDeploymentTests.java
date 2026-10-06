package com.plugpass;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class PrometheusDeploymentTests {
    @Test
    @DisplayName("관리 포트는 JVM과 수집 지표를 제공하며 앱 포트에는 지표를 노출하지 않는다")
    void exportsJvmAndIngestionMetrics() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(PlugPassApplication.class).run(
                "--server.port=0", "--management.server.port=0",
                "--management.endpoints.web.exposure.include=health,prometheus", "--logging.level.root=ERROR")) {
            int managementPort = Integer.parseInt(context.getEnvironment().getProperty("local.management.port"));
            int appPort = Integer.parseInt(context.getEnvironment().getProperty("local.server.port"));
            HttpResponse<String> response = get(managementPort, "/actuator/prometheus");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("jvm_memory_used_bytes", "plugpass_ingestion_has_success");
            assertThat(get(appPort, "/actuator/prometheus").statusCode()).isEqualTo(404);
            assertThat(get(managementPort, "/actuator/env").statusCode()).isEqualTo(404);
        }
    }

    private HttpResponse<String> get(int port, String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
