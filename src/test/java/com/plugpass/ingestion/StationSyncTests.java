package com.plugpass.ingestion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;
import com.plugpass.station.ChargerId;
import com.plugpass.station.ChargerRepository;
import com.plugpass.station.ChargerStatus;
import com.plugpass.station.GeoPoint;
import com.plugpass.station.StationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(StationSyncTests.ExternalBoundary.class)
class StationSyncTests {
    @Autowired private StationSyncService stationSyncService;
    @Autowired private StationUpsertService stationUpsertService;
    @Autowired private SyncRunRepository syncRunRepository;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private StationRepository stationRepository;
    @Autowired private PageClient pageClient;
    private final Instant collectedAt = Instant.parse("2026-10-02T00:00:00Z");

    @AfterEach
    void cleanOwnedState() {
        pageClient.pages.clear(); pageClient.requestedPages.clear(); pageClient.networkTransactions.clear();
        chargerRepository.deleteAllInBatch(); stationRepository.deleteAllInBatch(); syncRunRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("여러 페이지를 순서대로 commit하고 전체 성공 이력을 저장한다")
    void synchronizesAllPages() {
        List<StationSnapshot> firstPage = IntStream.rangeClosed(1,10).mapToObj(index -> snapshot("one-" + index,"2")).toList();
        StationSnapshot second = snapshot("two", "3");
        pageClient.pages.add(new StationPage(1, 10, 11, firstPage));
        pageClient.pages.add(new StationPage(2, 10, 11, List.of(second)));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(result.processedCount()).isEqualTo(11);
        assertThat(result.failedPage()).isNull();
        assertThat(pageClient.requestedPages).containsExactly(1, 2);
        assertThat(pageClient.networkTransactions).containsExactly(false, false);
        assertThat(chargerRepository.findAll()).extracting(charger -> charger.getId().stationId()).containsExactlyInAnyOrder("one-1","one-2","one-3","one-4","one-5","one-6","one-7","one-8","one-9","one-10","two");
        assertThat(syncRunRepository.findById(result.runId())).isPresent();
        SyncRun run = syncRunRepository.findById(result.runId()).orElseThrow();
        assertThat(run.getStartedAt()).isEqualTo(collectedAt);
        assertThat(run.getCompletedAt()).isEqualTo(collectedAt);
        assertThat(run.getStatus()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(run.getProcessedCount()).isEqualTo(11);
        assertThat(syncRunRepository.findFirstByStatusOrderByCompletedAtDesc(SyncStatus.SUCCESS)).isPresent();
    }

    @Test
    @DisplayName("같은 페이지를 재실행해도 업무 데이터는 중복되지 않고 실행 이력은 각각 남는다")
    void repeatsWithoutDuplicateRows() {
        StationSnapshot snapshot = snapshot("one", "2");
        pageClient.pages.add(new StationPage(1, 10, 1, List.of(snapshot)));
        stationSyncService.synchronize();
        pageClient.pages.add(new StationPage(1, 10, 1, List.of(snapshot)));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(stationRepository.count()).isEqualTo(1);
        assertThat(chargerRepository.count()).isEqualTo(1);
        assertThat(syncRunRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("중간 페이지 실패는 이미 commit한 페이지를 유지하며 부분 실패로 기록한다")
    void recordsPartialFailure() {
        pageClient.pages.add(new StationPage(1, 10, 11, List.of(snapshot("one", "2"))));
        pageClient.pages.add(new PublicDataException(PublicDataFailure.TIMEOUT));
        pageClient.pages.add(new PublicDataException(PublicDataFailure.TIMEOUT));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(SyncStatus.PARTIAL_FAILURE);
        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedPage()).isEqualTo(2);
        assertThat(result.failureCode()).isEqualTo("TIMEOUT");
        assertThat(pageClient.requestedPages).containsExactly(1,2,2);
        assertThat(result.failedPageCount()).isEqualTo(1);
        assertThat(syncRunRepository.findById(result.runId()).orElseThrow().getFailedPageCount()).isEqualTo(1);
        assertThat(chargerRepository.count()).isEqualTo(1);
        assertThat(syncRunRepository.findFirstByStatusOrderByCompletedAtDesc(SyncStatus.SUCCESS)).isEmpty();
        assertThat(syncRunRepository.findById(result.runId()).orElseThrow().getFailedPage()).isEqualTo(2);
    }

    @Test
    @DisplayName("첫 페이지 인증 실패는 기존 데이터와 마지막 성공 시각을 유지한다")
    void preservesPreviousSuccess() {
        pageClient.pages.add(new StationPage(1, 10, 1, List.of(snapshot("existing", "3"))));
        stationSyncService.synchronize();
        pageClient.pages.add(new PublicDataException(PublicDataFailure.AUTHENTICATION));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(SyncStatus.FAILURE);
        assertThat(result.processedCount()).isZero();
        assertThat(result.failureCode()).isEqualTo("AUTHENTICATION");
        assertThat(chargerRepository.count()).isEqualTo(1);
        assertThat(syncRunRepository.findFirstByStatusOrderByCompletedAtDesc(SyncStatus.SUCCESS)).isPresent();
        assertThat(syncRunRepository.findFirstByStatusOrderByCompletedAtDesc(SyncStatus.SUCCESS).orElseThrow().getCompletedAt()).isEqualTo(collectedAt);
    }

    @Test
    @DisplayName("빈 수집의 전체 성공도 데이터 준비 완료로 기록한다")
    void recordsEmptySuccess() {
        pageClient.pages.add(new StationPage(1, 10, 0, List.of()));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(result.processedCount()).isZero();
        assertThat(syncRunRepository.findFirstByStatusOrderByCompletedAtDesc(SyncStatus.SUCCESS)).isPresent();
    }

    @Test
    @DisplayName("페이지 안의 저장 실패는 해당 페이지 전체를 rollback한다")
    void rollsBackInvalidPage() {
        StationSnapshot valid = snapshot("valid", "2");
        StationSnapshot invalid = snapshot("invalid", "x".repeat(256));
        pageClient.pages.add(new StationPage(1, 10, 2, List.of(valid, invalid)));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(SyncStatus.FAILURE);
        assertThat(result.failureCode()).isEqualTo("STORAGE");
        assertThat(result.processedCount()).isZero();
        assertThat(stationRepository.count()).isZero();
        assertThat(chargerRepository.count()).isZero();
    }

    @Test
    @DisplayName("잘못된 레코드가 섞여 외부 계약에 실패한 페이지는 이전 데이터를 지우지 않는다")
    void preservesDataOnContractFailure() {
        stationUpsertService.upsert(snapshot("existing", "2"));
        pageClient.pages.add(new PublicDataException(PublicDataFailure.CONTRACT));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result).isNotNull();
        assertThat(result.failureCode()).isEqualTo("CONTRACT");
        assertThat(chargerRepository.count()).isEqualTo(1);
        assertThat(pageClient.requestedPages).containsExactly(1);
    }

    @Test
    @DisplayName("이전 페이지 번호를 다시 반환하면 중단하고 이미 저장한 데이터는 유지한다")
    void rejectsReversedPage() {
        pageClient.pages.add(new StationPage(1, 10, 11, List.of(snapshot("one", "2"))));
        pageClient.pages.add(new StationPage(1, 10, 11, List.of(snapshot("two", "3"))));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(SyncStatus.PARTIAL_FAILURE);
        assertThat(result.failureCode()).isEqualTo("CONTRACT");
        assertThat(chargerRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("중간 페이지의 전체 건수가 바뀌면 불완전한 수집을 전체 성공으로 표시하지 않는다")
    void rejectsChangedTotal() {
        pageClient.pages.add(new StationPage(1, 10, 11, List.of(snapshot("one", "2"))));
        pageClient.pages.add(new StationPage(2, 10, 12, List.of(snapshot("two", "3"))));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result.status()).isEqualTo(SyncStatus.PARTIAL_FAILURE);
        assertThat(result.failureCode()).isEqualTo("CONTRACT");
        assertThat(chargerRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("마지막 페이지까지 받았어도 전체 건수보다 적으면 성공으로 기록하지 않는다")
    void rejectsIncompleteRecordCount() {
        pageClient.pages.add(new StationPage(1,10,2,List.of(snapshot("one","2"))));
        SyncResult result = stationSyncService.synchronize();
        assertThat(result.status()).isEqualTo(SyncStatus.PARTIAL_FAILURE);
        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedPage()).isEqualTo(1);
        assertThat(result.failureCode()).isEqualTo("CONTRACT");
        assertThat(syncRunRepository.findFirstByStatusOrderByCompletedAtDesc(SyncStatus.SUCCESS)).isEmpty();
        assertThat(chargerRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("첫 페이지에 같은 충전기가 반복되면 페이지 전체를 저장하지 않고 실패한다")
    void rejectsDuplicateIdentityWithinPage() {
        StationSnapshot original = snapshot("duplicate", "2");
        StationSnapshot repeated = snapshot("duplicate", "3");
        pageClient.pages.add(new StationPage(1, 10, 2, List.of(original, repeated)));

        SyncResult result = stationSyncService.synchronize();

        assertThat(result.status()).isEqualTo(SyncStatus.FAILURE);
        assertThat(result.failureCode()).isEqualTo("CONTRACT");
        assertThat(result.processedCount()).isZero();
        assertThat(result.failedPage()).isEqualTo(1);
        assertThat(result.failedPageCount()).isEqualTo(1);
        assertThat(syncRunRepository.findById(result.runId()).orElseThrow().result()).isEqualTo(result);
        assertThat(syncRunRepository.findFirstByStatusOrderByCompletedAtDesc(SyncStatus.SUCCESS)).isEmpty();
        assertThat(chargerRepository.count()).isZero();
        assertThat(stationRepository.count()).isZero();
        assertThat(pageClient.requestedPages).containsExactly(1);
    }

    @Test
    @DisplayName("다음 페이지에 이전 충전기가 반복되면 새 항목과 덮어쓰기를 모두 거부하고 이전 페이지를 유지한다")
    void rejectsDuplicateIdentityAcrossPages() {
        List<StationSnapshot> firstPage = IntStream.rangeClosed(1, 10)
                .mapToObj(index -> snapshot("one-" + index, "2")).toList();
        StationSnapshot newCharger = snapshot("new", "2");
        StationSnapshot repeated = snapshot("one-1", "3");
        pageClient.pages.add(new StationPage(1, 10, 12, firstPage));
        pageClient.pages.add(new StationPage(2, 10, 12, List.of(newCharger, repeated)));

        SyncResult result = stationSyncService.synchronize();

        assertThat(result.status()).isEqualTo(SyncStatus.PARTIAL_FAILURE);
        assertThat(result.failureCode()).isEqualTo("CONTRACT");
        assertThat(result.processedCount()).isEqualTo(10);
        assertThat(result.failedPage()).isEqualTo(2);
        assertThat(result.failedPageCount()).isEqualTo(1);
        assertThat(syncRunRepository.findById(result.runId()).orElseThrow().result()).isEqualTo(result);
        assertThat(syncRunRepository.findFirstByStatusOrderByCompletedAtDesc(SyncStatus.SUCCESS)).isEmpty();
        assertThat(chargerRepository.findAll()).extracting(charger -> charger.getId().stationId())
                .containsExactlyInAnyOrder("one-1", "one-2", "one-3", "one-4", "one-5", "one-6", "one-7", "one-8", "one-9", "one-10");
        assertThat(stationRepository.count()).isEqualTo(10);
        assertThat(chargerRepository.findByIdentity(repeated.chargerId()).orElseThrow().getRawStatus()).isEqualTo("2");
        assertThat(pageClient.requestedPages).containsExactly(1, 2);
    }

    private StationSnapshot snapshot(String stationId, String rawStatus) {
        return new StationSnapshot(new ChargerId("ME", stationId, "01"), "충전소", new GeoPoint(37.5, 126.6),
                ChargerStatus.AVAILABLE, rawStatus, null, null, collectedAt);
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class ExternalBoundary {
        @Bean @Primary PageClient pageClient() { return new PageClient(); }
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-10-02T00:00:00Z"), ZoneOffset.UTC); }
    }
    static class PageClient implements PublicDataClient {
        final ArrayDeque<Object> pages = new ArrayDeque<>();
        final List<Integer> requestedPages = new ArrayList<>();
        final List<Boolean> networkTransactions = new ArrayList<>();
        public StationPage fetchPage(int page) {
            requestedPages.add(page);
            networkTransactions.add(TransactionSynchronizationManager.isActualTransactionActive());
            Object response = pages.remove();
            if (response instanceof RuntimeException failure) { throw failure; }
            return (StationPage) response;
        }
        public StationPage fetchPage(int page, Duration remaining) { return fetchPage(page); }
        public void close() { }
    }
}
