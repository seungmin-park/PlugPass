package com.plugpass.ingestion.service;

import com.plugpass.ingestion.dto.StationPage;
import com.plugpass.ingestion.dto.SyncResult;
import com.plugpass.ingestion.dto.StationSnapshot;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.client.PublicDataClient;
import com.plugpass.ingestion.client.IngestionTime;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;
import com.plugpass.freshness.domain.Freshness;
import com.plugpass.station.domain.Connector;
import com.plugpass.search.service.StationQueryService;
import com.plugpass.search.domain.StationSearchQuery;
import com.plugpass.station.domain.ChargerDetails;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.domain.GeoPoint;
import com.plugpass.station.repository.StationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"plugpass.ingestion.max-pages=2","plugpass.ingestion.max-requests=3", "plugpass.ingestion.run-timeout=PT5S", "plugpass.ingestion.retry-delay=PT1S"})
@Import(IngestionRecoveryTests.ExternalBoundary.class)
class IngestionRecoveryTests {
    @Autowired private StationSyncService stationSyncService;
    @Autowired private StationQueryService stationQueryService;
    @Autowired private StationUpsertService stationUpsertService;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private StationRepository stationRepository;
    @Autowired private SyncRunRepository syncRunRepository;
    @Autowired private ScriptedClient scriptedClient;
    @Autowired private ControlledTime controlledTime;
    @AfterEach
    void deleteOwnedState() {
        chargerRepository.deleteAllInBatch(); stationRepository.deleteAllInBatch(); syncRunRepository.deleteAllInBatch();
        scriptedClient.reset(); controlledTime.reset();
    }
    @ParameterizedTest
    @DisplayName("일시 장애는 한 번만 재시도하고 종료하며 인증·계약 오류는 반복하지 않는다")
    @MethodSource("failureCases")
    void boundsRetries(PublicDataFailure failure, List<Integer> expectedRequests) {
        scriptedClient.fallbackFailure = new PublicDataException(failure);
        SyncResult result = stationSyncService.synchronize();
        assertThat(result.status()).isEqualTo(SyncStatus.FAILURE);
        assertThat(result.failureCode()).isEqualTo(failure.name());
        assertThat(scriptedClient.requests).containsExactlyElementsOf(expectedRequests);
        assertThat(result.requestCount()).isEqualTo(expectedRequests.size());
        assertThat(result.retryCount()).isEqualTo(expectedRequests.size()-1);
        assertThat(syncRunRepository.findById(result.runId()).orElseThrow().getRequestCount()).isEqualTo(expectedRequests.size());
        assertThat(controlledTime.nanoTime()).isLessThanOrEqualTo(Duration.ofSeconds(5).toNanos());
        assertThat(syncRunRepository.findById(result.runId()).orElseThrow().getStatus()).isEqualTo(SyncStatus.FAILURE);
    }
    static Stream<Arguments> failureCases() {
        return Stream.of(Arguments.of(PublicDataFailure.TIMEOUT,List.of(1,1)),Arguments.of(PublicDataFailure.RATE_LIMIT,List.of(1,1)),
                Arguments.of(PublicDataFailure.SERVER,List.of(1,1)),Arguments.of(PublicDataFailure.TRANSPORT,List.of(1,1)),
                Arguments.of(PublicDataFailure.AUTHENTICATION,List.of(1)),Arguments.of(PublicDataFailure.CONTRACT,List.of(1)));
    }
    @Test
    @DisplayName("예산 안의 Retry-After를 기다린 뒤 같은 페이지를 재요청한다")
    void honorsRetryAfterWithinBudget() {
        scriptedClient.responses.add(new PublicDataException(PublicDataFailure.RATE_LIMIT,Duration.ofSeconds(2)));
        scriptedClient.responses.add(new StationPage(1,10,0,List.of()));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result.status()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(scriptedClient.requests).containsExactly(1,1);
        assertThat(controlledTime.waits).containsExactly(Duration.ofSeconds(2));
    }
    @Test
    @DisplayName("Retry-After가 남은 예산과 같거나 크면 기다리거나 재요청하지 않는다")
    void stopsRetryAfterOutsideBudget() {
        scriptedClient.fallbackFailure = new PublicDataException(PublicDataFailure.RATE_LIMIT,Duration.ofSeconds(5));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result.status()).isEqualTo(SyncStatus.FAILURE);
        assertThat(result.failureCode()).isEqualTo("RATE_LIMIT");
        assertThat(scriptedClient.requests).containsExactly(1);
        assertThat(controlledTime.waits).isEmpty();
    }
    @Test
    @DisplayName("늦게 끝난 요청은 전체 시간 예산을 다시 검사해 저장하지 않는다")
    void rejectsResponseAfterDeadline() {
        scriptedClient.elapsedPerRequest = Duration.ofSeconds(5);
        scriptedClient.responses.add(new StationPage(1,10,1,List.of(snapshot("late",ChargerStatus.AVAILABLE))));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result.status()).isEqualTo(SyncStatus.FAILURE);
        assertThat(result.failureCode()).isEqualTo("BUDGET_EXHAUSTED");
        assertThat(scriptedClient.requests).containsExactly(1);
        assertThat(chargerRepository.count()).isZero();
    }
    @Test
    @DisplayName("페이지 상한을 넘으면 다음 요청 없이 이미 commit한 데이터를 보존한다")
    void boundsPages() {
        scriptedClient.responses.add(new StationPage(1,10,21,IntStream.rangeClosed(1,10).mapToObj(index -> snapshot("first-"+index,ChargerStatus.AVAILABLE)).toList()));
        scriptedClient.responses.add(new StationPage(2,10,21,IntStream.rangeClosed(1,10).mapToObj(index -> snapshot("second-"+index,ChargerStatus.AVAILABLE)).toList()));
        scriptedClient.responses.add(new StationPage(3,10,21,List.of(snapshot("third",ChargerStatus.AVAILABLE))));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result.status()).isEqualTo(SyncStatus.PARTIAL_FAILURE);
        assertThat(result.failureCode()).isEqualTo("BUDGET_EXHAUSTED");
        assertThat(result.processedCount()).isEqualTo(20);
        assertThat(scriptedClient.requests).containsExactly(1,2);
        assertThat(chargerRepository.count()).isEqualTo(20);
    }
    @Test
    @DisplayName("재시도도 요청 예산에 포함하여 상한 뒤 네 번째 호출을 막는다")
    void boundsTotalRequests() {
        scriptedClient.responses.add(new PublicDataException(PublicDataFailure.SERVER));
        scriptedClient.responses.add(new StationPage(1,10,11,IntStream.rangeClosed(1,10).mapToObj(index -> snapshot("first-"+index,ChargerStatus.AVAILABLE)).toList()));
        scriptedClient.fallbackFailure = new PublicDataException(PublicDataFailure.SERVER);
        SyncResult result = stationSyncService.synchronize();
        assertThat(result.status()).isEqualTo(SyncStatus.PARTIAL_FAILURE);
        assertThat(result.failureCode()).isEqualTo("BUDGET_EXHAUSTED");
        assertThat(scriptedClient.requests).containsExactly(1,1,2);
        assertThat(result.processedCount()).isEqualTo(10);
    }
    @Test
    @DisplayName("페이지가 진행하지 않으면 계약 실패로 끝내며 재시도하지 않는다")
    void rejectsNonProgressingPage() {
        scriptedClient.responses.add(new StationPage(1,10,11,IntStream.rangeClosed(1,10).mapToObj(index -> snapshot("first-"+index,ChargerStatus.AVAILABLE)).toList()));
        scriptedClient.responses.add(new StationPage(1,10,11,List.of(snapshot("wrong",ChargerStatus.AVAILABLE))));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result.status()).isEqualTo(SyncStatus.PARTIAL_FAILURE);
        assertThat(result.failureCode()).isEqualTo("CONTRACT");
        assertThat(scriptedClient.requests).containsExactly(1,2);
        assertThat(chargerRepository.count()).isEqualTo(10);
    }
    @Test
    @DisplayName("장애 중 기존 데이터를 읽되 시간이 지나면 오래됨으로 낮추고 다음 회차에 복구한다")
    void readsAgingDataAndRecovers() {
        scriptedClient.responses.add(new StationPage(1,10,1,List.of(snapshot("saved",ChargerStatus.AVAILABLE))));
        SyncResult initial = stationSyncService.synchronize();
        assertThat(initial.status()).isEqualTo(SyncStatus.SUCCESS);
        Instant lastSuccess = syncRunRepository.findById(initial.runId()).orElseThrow().getCompletedAt();
        controlledTime.advance(Duration.ofMinutes(11));
        scriptedClient.fallbackFailure = new PublicDataException(PublicDataFailure.SERVER);
        assertThat(stationSyncService.synchronize().status()).isEqualTo(SyncStatus.FAILURE);
        assertThat(stationQueryService.search(new StationSearchQuery(new GeoPoint(0,0),1000,Connector.DC_COMBO,20)).stations()
                .getFirst().chargers().getFirst().freshness().freshness()).isEqualTo(Freshness.STALE);
        assertThat(stationQueryService.search(new StationSearchQuery(new GeoPoint(0,0),1000,Connector.DC_COMBO,20)).lastSuccessfulRunAt()).isEqualTo(lastSuccess);
        SyncResult repeatedFailure = stationSyncService.synchronize();
        assertThat(repeatedFailure.status()).isEqualTo(SyncStatus.FAILURE);
        assertThat(repeatedFailure.requestCount()).isEqualTo(2);
        assertThat(repeatedFailure.retryCount()).isEqualTo(1);
        assertThat(chargerRepository.count()).isEqualTo(1);
        assertThat(stationQueryService.search(new StationSearchQuery(new GeoPoint(0,0),1000,Connector.DC_COMBO,20)).lastSuccessfulRunAt()).isEqualTo(lastSuccess);
        scriptedClient.fallbackFailure = null;
        scriptedClient.responses.add(new StationPage(1,10,1,List.of(snapshot("saved",ChargerStatus.OCCUPIED))));
        SyncResult recovered = stationSyncService.synchronize();
        assertThat(recovered.status()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(stationRepository.count()).isEqualTo(1);
        assertThat(stationQueryService.search(new StationSearchQuery(new GeoPoint(0,0),1000,Connector.DC_COMBO,20)).lastSuccessfulRunAt())
                .isEqualTo(syncRunRepository.findById(recovered.runId()).orElseThrow().getCompletedAt()).isAfter(lastSuccess);
        assertThat(chargerRepository.count()).isEqualTo(1);
        assertThat(chargerRepository.findByIdentity(new ChargerId("ME","saved","01")).orElseThrow().getStatus()).isEqualTo(ChargerStatus.OCCUPIED);
        assertThat(stationQueryService.search(new StationSearchQuery(new GeoPoint(0,0),1000,Connector.DC_COMBO,20)).stations()
                .getFirst().chargers().getFirst().freshness().freshness()).isEqualTo(Freshness.RECENT);
    }
    @Test
    @DisplayName("인터럽트 종료는 재시도하지 않고 플래그를 유지한다")
    void doesNotRetryInterruptedRequest() {
        scriptedClient.fallbackFailure = new PublicDataException(PublicDataFailure.TRANSPORT);
        Thread.currentThread().interrupt();
        try {
            assertThat(stationSyncService.synchronize().failureCode()).isEqualTo("TRANSPORT");
            assertThat(scriptedClient.requests).containsExactly(1);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally { Thread.interrupted(); }
    }
    private StationSnapshot snapshot(String stationId, ChargerStatus status) {
        ChargerDetails details = new ChargerDetails("04","24시간","N",null,null,null,null,null,null);
        return new StationSnapshot(new ChargerId("ME",stationId,"01"),stationId,new GeoPoint(0,0),status,"2",details,controlledTime.instant(),controlledTime.instant());
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class ExternalBoundary {
        @Bean @Primary ControlledTime controlledTime() { return new ControlledTime(); }
        @Bean @Primary ScriptedClient scriptedClient(ControlledTime time) { return new ScriptedClient(time); }
    }
    static class ControlledTime extends Clock implements IngestionTime {
        Instant now = Instant.parse("2026-10-05T00:00:00Z"); long nanos;
        final List<Duration> waits = new ArrayList<>();
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return Clock.fixed(now,zone); }
        public Instant instant() { return now; }
        public long nanoTime() { return nanos; }
        public void sleep(Duration duration) { waits.add(duration); advance(duration); }
        void advance(Duration duration) { now = now.plus(duration); nanos += duration.toNanos(); }
        void reset() { now = Instant.parse("2026-10-05T00:00:00Z"); nanos = 0; waits.clear(); }
    }
    static class ScriptedClient implements PublicDataClient {
        final ControlledTime time;
        final ArrayDeque<Object> responses = new ArrayDeque<>();
        final List<Integer> requests = new ArrayList<>();
        PublicDataException fallbackFailure;
        Duration elapsedPerRequest = Duration.ZERO;
        ScriptedClient(ControlledTime time) { this.time = time; }
        public StationPage fetchPage(int page) {
            requests.add(page); time.advance(elapsedPerRequest);
            Object response = responses.poll();
            if (response == null) { throw fallbackFailure; }
            if (response instanceof PublicDataException failure) { throw failure; }
            return (StationPage)response;
        }
        void reset() { responses.clear(); requests.clear(); fallbackFailure = null; elapsedPerRequest = Duration.ZERO; }
        public StationPage fetchPage(int page, Duration remaining) { return fetchPage(page); }
        public void close() { }
    }
}
