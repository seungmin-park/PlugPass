package com.plugpass.ingestion.service;

import com.plugpass.ingestion.client.PublicStatusClient;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;
import com.plugpass.ingestion.domain.SyncKind;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.dto.ChargerStatusSnapshot;
import com.plugpass.ingestion.dto.StatusPage;
import com.plugpass.ingestion.dto.StationSnapshot;
import com.plugpass.ingestion.dto.SyncResult;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.domain.GeoPoint;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.repository.StationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.ArrayDeque;
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
@Import(StatusSyncTests.ExternalBoundary.class)
class StatusSyncTests {
    @Autowired private StatusSyncService statusSyncService;
    @Autowired private StationUpsertService stationUpsertService;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private StationRepository stationRepository;
    @Autowired private SyncRunRepository syncRunRepository;
    @Autowired private StatusClient statusClient;
    @AfterEach void cleanOwnedData() {
        statusClient.pages.clear(); chargerRepository.deleteAllInBatch(); stationRepository.deleteAllInBatch(); syncRunRepository.deleteAllInBatch();
    }
    @Test
    @DisplayName("상태 변경 페이지를 실제 저장하고 기본정보 성공과 분리된 이력을 남긴다")
    void storesStatusAndSeparateSuccess() {
        Instant received = Instant.parse("2026-10-13T00:00:00Z");
        ChargerId identity = new ChargerId("ME","001","01");
        stationUpsertService.upsert(new StationSnapshot(identity,"서울 충전소",new GeoPoint(37.5,127),ChargerStatus.AVAILABLE,"2",null,null,received));
        statusClient.pages.add(new StatusPage(1,10,1,List.of(new ChargerStatusSnapshot(identity,ChargerStatus.OCCUPIED,"3","20261013090000",null,null,null,received.plusSeconds(10)))));
        SyncResult result = statusSyncService.synchronize();
        assertThat(result.status()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(chargerRepository.findByIdentity(identity).orElseThrow().getStatus()).isEqualTo(ChargerStatus.OCCUPIED);
        assertThat(syncRunRepository.findById(result.runId()).orElseThrow().getKind()).isEqualTo(SyncKind.STATUS);
        assertThat(syncRunRepository.findFirstByKindAndStatusOrderByCompletedAtDesc(SyncKind.CATALOG,SyncStatus.SUCCESS)).isEmpty();
    }
    @Test
    @DisplayName("상태 API 실패는 저장된 상태와 직전 성공 시각을 유지하고 다음 회차에 복구된다")
    void preservesDataAndRecoversAfterProviderFailure() {
        Instant received = Instant.parse("2026-10-13T00:00:00Z");
        ChargerId identity = new ChargerId("ME","001","01");
        stationUpsertService.upsert(new StationSnapshot(identity,"서울 충전소",new GeoPoint(37.5,127),ChargerStatus.AVAILABLE,"2",null,null,received));
        statusClient.pages.add(new StatusPage(1,10,0,List.of()));
        SyncResult success = statusSyncService.synchronize();
        statusClient.pages.add(new PublicDataException(PublicDataFailure.SERVER));
        statusClient.pages.add(new PublicDataException(PublicDataFailure.SERVER));
        SyncResult failure = statusSyncService.synchronize();
        assertThat(failure.status()).isEqualTo(SyncStatus.FAILURE);
        assertThat(failure.failureCode()).isEqualTo("SERVER");
        assertThat(failure.requestCount()).isEqualTo(2);
        assertThat(failure.retryCount()).isEqualTo(1);
        assertThat(chargerRepository.findByIdentity(identity).orElseThrow().getStatus()).isEqualTo(ChargerStatus.AVAILABLE);
        assertThat(syncRunRepository.findFirstByKindAndStatusOrderByCompletedAtDesc(SyncKind.STATUS,SyncStatus.SUCCESS).orElseThrow().getId()).isEqualTo(success.runId());
        statusClient.pages.add(new StatusPage(1,10,0,List.of()));
        assertThat(statusSyncService.synchronize().status()).isEqualTo(SyncStatus.SUCCESS);
    }
    @Test
    @DisplayName("중복 식별자가 있는 상태 페이지는 저장 전에 계약 실패로 거부한다")
    void rejectsDuplicateStatusIdentity() {
        ChargerStatusSnapshot snapshot = new ChargerStatusSnapshot(new ChargerId("ME","001","01"),ChargerStatus.OCCUPIED,"3","20261013090000",null,null,null,Instant.EPOCH);
        statusClient.pages.add(new StatusPage(1,10,2,List.of(snapshot,snapshot)));
        SyncResult result = statusSyncService.synchronize();
        assertThat(result.failureCode()).isEqualTo("CONTRACT");
        assertThat(result.processedCount()).isZero();
        assertThat(chargerRepository.findAll()).isEmpty();
    }
    @TestConfiguration(proxyBeanMethods=false)
    static class ExternalBoundary {
        @Bean @Primary StatusClient statusClient() { return new StatusClient(); }
    }
    static class StatusClient implements PublicStatusClient {
        final ArrayDeque<Object> pages = new ArrayDeque<>();
        public StatusPage fetchStatusPage(int page, Duration remaining) {
            Object response = pages.removeFirst();
            if (response instanceof RuntimeException failure) { throw failure; }
            return (StatusPage) response;
        }
    }
}
