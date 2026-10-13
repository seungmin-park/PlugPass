package com.plugpass.ingestion.service;

import com.plugpass.ingestion.domain.SyncRun;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.search.domain.StationSearchQuery;
import com.plugpass.search.service.StationQueryService;
import com.plugpass.station.domain.Connector;
import com.plugpass.station.domain.GeoPoint;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CollectionReadinessTests {
    @Autowired private StationQueryService stationQueryService;
    @Autowired private SyncRunRepository syncRunRepository;
    @AfterEach void cleanOwnedData() { syncRunRepository.deleteAllInBatch(); }
    @Test
    @DisplayName("상태 변경분만 성공한 경우 전체 충전소 데이터 준비 완료로 표시하지 않는다")
    void statusSuccessDoesNotEstablishCatalogReadiness() {
        Instant started = Instant.parse("2026-10-13T00:00:00Z");
        SyncRun status = SyncRun.statusRun(started);
        status.complete(0,null,null,started.plusSeconds(1));
        syncRunRepository.save(status);
        assertThat(stationQueryService.search(new StationSearchQuery(new GeoPoint(37.5,127),1000,Connector.DC_COMBO,20)).dataReady()).isFalse();
    }
}
