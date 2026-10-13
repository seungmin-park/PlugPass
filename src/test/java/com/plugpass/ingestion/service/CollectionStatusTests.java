package com.plugpass.ingestion.service;

import com.plugpass.ingestion.domain.SyncRun;
import com.plugpass.ingestion.dto.CollectionStatus;
import com.plugpass.ingestion.repository.SyncRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(CollectionStatusTests.TimeBoundary.class)
class CollectionStatusTests {
    private static final Instant NOW = Instant.parse("2026-10-13T00:00:00Z");
    @Autowired private CollectionStatusService collectionStatusService;
    @Autowired private SyncRunRepository syncRunRepository;
    @AfterEach void cleanOwnedData() { syncRunRepository.deleteAllInBatch(); }
    @Test
    @DisplayName("아직 수집을 실행하지 않았으면 준비 전이며 실행 중이라고 표시하지 않는다")
    void reportsNotReady() {
        assertThat(collectionStatusService.current()).isEqualTo(new CollectionStatus("NOT_READY",null,null,null,null,300));
    }
    @Test
    @DisplayName("최근 전체 수집에 성공하면 수집 정상과 정확한 성공 시각을 반환한다")
    void reportsCurrentCollection() {
        SyncRun catalog = SyncRun.builder().startedAt(NOW.minusSeconds(120)).build();
        catalog.complete(1,null,null,NOW.minusSeconds(60));
        syncRunRepository.save(catalog);
        assertThat(collectionStatusService.current()).isEqualTo(new CollectionStatus("CURRENT",NOW.minusSeconds(60),null,NOW.minusSeconds(120),"SUCCESS",300));
    }
    @Test
    @DisplayName("상태 수집이 실패하면 이전 성공 시각을 유지하고 갱신 지연을 표시한다")
    void reportsFailureWithoutErasingSuccess() {
        SyncRun catalog = SyncRun.builder().startedAt(NOW.minusSeconds(120)).build();
        catalog.complete(1,null,null,NOW.minusSeconds(60));
        syncRunRepository.save(catalog);
        SyncRun failure = SyncRun.statusRun(NOW.minusSeconds(30));
        failure.complete(0,1,"SERVER",NOW.minusSeconds(20));
        syncRunRepository.save(failure);
        assertThat(collectionStatusService.current()).isEqualTo(new CollectionStatus("DELAYED",NOW.minusSeconds(60),null,NOW.minusSeconds(30),"FAILURE",300));
    }
    @Test
    @DisplayName("성공 이후 10분이 지나면 이전 데이터를 유지하되 갱신 지연을 표시한다")
    void reportsMissedWindow() {
        SyncRun catalog = SyncRun.builder().startedAt(NOW.minusSeconds(700)).build();
        catalog.complete(1,null,null,NOW.minusSeconds(600));
        syncRunRepository.save(catalog);
        assertThat(collectionStatusService.current().state()).isEqualTo("DELAYED");
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class TimeBoundary {
        @Bean @Primary Clock collectionClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
    }
}
