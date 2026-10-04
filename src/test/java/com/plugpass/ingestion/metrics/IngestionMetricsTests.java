package com.plugpass.ingestion.metrics;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.stream.IntStream;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;
import com.plugpass.ingestion.client.PublicDataClient;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.dto.StationPage;
import com.plugpass.ingestion.dto.StationSnapshot;
import com.plugpass.ingestion.dto.SyncResult;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.ingestion.scheduler.SyncScheduler;
import com.plugpass.ingestion.service.StationSyncService;
import com.plugpass.ingestion.service.StationUpsertService;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.domain.GeoPoint;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.repository.StationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.endpoints.web.exposure.include=health,metrics", "plugpass.ingestion.retry-delay=PT0S"})
@Import(IngestionMetricsTests.ExternalBoundary.class)
@ExtendWith(OutputCaptureExtension.class)
class IngestionMetricsTests {
    @Autowired private StationSyncService stationSyncService;
    @Autowired private StationUpsertService stationUpsertService;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private StationRepository stationRepository;
    @Autowired private SyncRunRepository syncRunRepository;
    @Autowired private MeterRegistry meterRegistry;
    @Autowired private MetricsClient metricsClient;
    @Autowired private MutableClock mutableClock;
    @Autowired private JsonMapper jsonMapper;
    @LocalServerPort private int port;
    @AfterEach
    void deleteOwnedState() {
        chargerRepository.deleteAllInBatch(); stationRepository.deleteAllInBatch(); syncRunRepository.deleteAllInBatch();
        meterRegistry.getMeters().stream().filter(meter -> meter.getId().getName().startsWith("plugpass.")
                && meter.getId().getType() == Meter.Type.COUNTER).toList().forEach(meterRegistry::remove);
        metricsClient.responses.clear(); metricsClient.failure = null; mutableClock.reset();
    }
    @Test
    @DisplayName("성공한 회차와 commit한 입력 건수·요청 수를 구분해 센다")
    void countsSuccessAndCommittedRecords() {
        metricsClient.responses.add(new StationPage(1,10,1,List.of(snapshot("one",mutableClock.instant()))));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result.status()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(chargerRepository.count()).isEqualTo(1);
        Counter runs = meterRegistry.find("plugpass.ingestion.runs").tag("outcome","SUCCESS").counter();
        assertThat(runs).isNotNull(); assertThat(runs.count()).isEqualTo(1);
        assertThat(counter("plugpass.ingestion.records").count()).isEqualTo(1);
        assertThat(counter("plugpass.ingestion.requests").count()).isEqualTo(1);
        assertThat(counter("plugpass.ingestion.retries").count()).isZero();
    }
    @Test
    @DisplayName("중간 실패는 전체 실패와 구분하고 commit한 건수와 실패 원인을 남긴다")
    void countsPartialFailure() {
        metricsClient.responses.add(new StationPage(1,10,11,IntStream.rangeClosed(1,10)
                .mapToObj(index -> snapshot("one-"+index,mutableClock.instant())).toList()));
        metricsClient.failure = new PublicDataException(PublicDataFailure.AUTHENTICATION);
        SyncResult result = stationSyncService.synchronize();
        assertThat(result.status()).isEqualTo(SyncStatus.PARTIAL_FAILURE);
        Counter runs = meterRegistry.find("plugpass.ingestion.runs").tag("outcome","PARTIAL_FAILURE").counter();
        assertThat(runs).isNotNull(); assertThat(runs.count()).isEqualTo(1);
        Counter failures = meterRegistry.find("plugpass.ingestion.failures").tag("cause","AUTHENTICATION").counter();
        assertThat(failures).isNotNull(); assertThat(failures.count()).isEqualTo(1);
        assertThat(counter("plugpass.ingestion.records").count()).isEqualTo(10);
        assertThat(chargerRepository.count()).isEqualTo(10);
    }
    @Test
    @DisplayName("전체 실패와 재시도는 별도로 세고 성공 전 경과 시각을 미확인으로 표시한다")
    void countsFailureAndRetries() {
        metricsClient.failure = new PublicDataException(PublicDataFailure.SERVER);
        SyncResult result = stationSyncService.synchronize();
        assertThat(result.status()).isEqualTo(SyncStatus.FAILURE);
        Counter runs = meterRegistry.find("plugpass.ingestion.runs").tag("outcome","FAILURE").counter();
        assertThat(runs).isNotNull(); assertThat(runs.count()).isEqualTo(1);
        assertThat(counter("plugpass.ingestion.requests").count()).isEqualTo(2);
        assertThat(counter("plugpass.ingestion.retries").count()).isEqualTo(1);
        assertThat(gauge("plugpass.ingestion.has.success").value()).isZero();
        assertThat(Double.isNaN(gauge("plugpass.ingestion.last.success.age").value())).isTrue();
    }
    @Test
    @DisplayName("실패가 마지막 성공 시계를 초기화하지 않고 다음 성공에만 갱신한다")
    void agesLastSuccessAcrossFailure() throws Exception {
        metricsClient.responses.add(new StationPage(1,10,0,List.of()));
        stationSyncService.synchronize();
        assertThat(gauge("plugpass.ingestion.last.success.age").value()).isZero();
        mutableClock.advance(Duration.ofMinutes(2));
        metricsClient.failure = new PublicDataException(PublicDataFailure.SERVER);
        stationSyncService.synchronize();
        assertThat(gauge("plugpass.ingestion.has.success").value()).isEqualTo(1);
        assertThat(gauge("plugpass.ingestion.last.success.age").value()).isEqualTo(120);
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> age = get(client,"/actuator/metrics/plugpass.ingestion.last.success.age");
            assertThat(age.statusCode()).isEqualTo(200);
            assertThat(metricValue(age)).isEqualTo(120);
        }
        metricsClient.failure = null;
        metricsClient.responses.add(new StationPage(1,10,0,List.of()));
        stationSyncService.synchronize();
        assertThat(gauge("plugpass.ingestion.last.success.age").value()).isZero();
    }
    @Test
    @DisplayName("health UP과 별개로 최신·오래됨·미확인 분포를 실제 HTTP로 확인한다")
    void exposesQualityDistributionOverHttp() throws Exception {
        stationUpsertService.upsert(snapshot("recent",mutableClock.instant()));
        stationUpsertService.upsert(snapshot("boundary",mutableClock.instant().minusSeconds(600)));
        stationUpsertService.upsert(snapshot("stale",mutableClock.instant().minusSeconds(601)));
        stationUpsertService.upsert(snapshot("missing",null));
        stationUpsertService.upsert(snapshot("future",mutableClock.instant().plusSeconds(1)));
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> health = get(client,"/actuator/health");
            assertThat(health.statusCode()).isEqualTo(200);
            assertThat(jsonMapper.readTree(health.body()).path("status").asString()).isEqualTo("UP");
            HttpResponse<String> recent = get(client,"/actuator/metrics/plugpass.data.chargers?tag=freshness:RECENT");
            assertThat(recent.statusCode()).isEqualTo(200);
            assertThat(metricValue(recent)).isEqualTo(2);
            HttpResponse<String> stale = get(client,"/actuator/metrics/plugpass.data.chargers?tag=freshness:STALE");
            assertThat(stale.statusCode()).isEqualTo(200); assertThat(metricValue(stale)).isEqualTo(1);
            HttpResponse<String> unverified = get(client,"/actuator/metrics/plugpass.data.chargers?tag=freshness:UNVERIFIED");
            assertThat(unverified.statusCode()).isEqualTo(200); assertThat(metricValue(unverified)).isEqualTo(2);
            Path evidence = Path.of("build/verification/ingestion-metrics"); Files.createDirectories(evidence);
            Files.writeString(evidence.resolve("health.json"),health.body());
            Files.writeString(evidence.resolve("recent.json"),recent.body()); Files.writeString(evidence.resolve("stale.json"),stale.body());
            Files.writeString(evidence.resolve("unverified.json"),unverified.body());
        }
        assertThat(meterRegistry.getMeters().stream().filter(meter -> meter.getId().getName().startsWith("plugpass.")))
                .allSatisfy(meter -> assertThat(meter.getId().getTags()).allSatisfy(tag -> assertThat(tag.getKey()).isIn("outcome","cause","freshness")));
        mutableClock.advance(Duration.ofMinutes(11));
        assertThat(meterRegistry.get("plugpass.data.chargers").tag("freshness","RECENT").gauge().value()).isZero();
        assertThat(meterRegistry.get("plugpass.data.chargers").tag("freshness","STALE").gauge().value()).isEqualTo(4);
        assertThat(meterRegistry.get("plugpass.data.chargers").tag("freshness","UNVERIFIED").gauge().value()).isEqualTo(1);
    }
    @Test
    @DisplayName("실행 ID·건수·원인을 로그에 연결하고 원본 예외와 비밀값은 내보내지 않는다")
    void logsSafeCompletion(CapturedOutput output) {
        metricsClient.failure = new IllegalStateException("fixture-secret-url-and-body");
        new SyncScheduler(stationSyncService).tick();
        assertThat(output).contains("ingestion runId=","outcome=FAILURE","processed=0","failure=INTERNAL","requests=1","retries=0")
                .doesNotContain("fixture-secret-url-and-body");
        assertThat(syncRunRepository.findAll()).hasSize(1);
    }
    private Counter counter(String name) {
        Counter counter = meterRegistry.find(name).counter(); assertThat(counter).isNotNull(); return counter;
    }
    private Gauge gauge(String name) {
        Gauge gauge = meterRegistry.find(name).gauge(); assertThat(gauge).isNotNull(); return gauge;
    }
    private double metricValue(HttpResponse<String> response) throws Exception {
        JsonNode body = jsonMapper.readTree(response.body());
        assertThat(body.path("measurements").size()).isEqualTo(1);
        return body.path("measurements").get(0).path("value").asDouble();
    }
    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(5)).GET().build(),HttpResponse.BodyHandlers.ofString());
    }
    private StationSnapshot snapshot(String stationId, Instant observedAt) {
        return new StationSnapshot(new ChargerId("ME",stationId,"01"),stationId,new GeoPoint(0,0),ChargerStatus.AVAILABLE,"2",null,observedAt,mutableClock.instant());
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class ExternalBoundary {
        @Bean @Primary MutableClock mutableClock() { return new MutableClock(); }
        @Bean @Primary MetricsClient metricsClient() { return new MetricsClient(); }
    }
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-05T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return Clock.fixed(now,zone); }
        public Instant instant() { return now; }
        void advance(Duration duration) { now = now.plus(duration); }
        void reset() { now = Instant.parse("2026-10-05T00:00:00Z"); }
    }
    static class MetricsClient implements PublicDataClient {
        final ArrayDeque<Object> responses = new ArrayDeque<>(); RuntimeException failure;
        public StationPage fetchPage(int page) {
            Object response = responses.poll();
            if (response == null) { throw failure; }
            return (StationPage)response;
        }
        public StationPage fetchPage(int page, Duration remaining) { return fetchPage(page); }
        public void close() { }
    }
}
