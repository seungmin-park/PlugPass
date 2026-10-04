package com.plugpass.search.service;

import com.plugpass.ingestion.dto.StationSnapshot;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.ingestion.service.StationUpsertService;
import com.plugpass.search.domain.StationSearchQuery;
import com.plugpass.search.dto.StationMatch;
import com.plugpass.search.dto.StationSearchResult;
import com.plugpass.station.domain.ChargerDetails;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.domain.Connector;
import com.plugpass.station.domain.GeoPoint;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.repository.StationRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:query-efficiency",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"})
class StationQueryEfficiencyTests {
    @Autowired private StationQueryService stationQueryService;
    @Autowired private StationUpsertService stationUpsertService;
    @Autowired private StationRepository stationRepository;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private SyncRunRepository syncRunRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @AfterEach
    void deleteOwnedData() {
        chargerRepository.deleteAllInBatch();
        stationRepository.deleteAllInBatch();
        syncRunRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("검색은 반경 후보의 호환 충전기만 로딩하며 SQL 수와 반환 계약을 유지한다")
    void loadsOnlyCompatibleNearbyCandidates() {
        saveCharger("near", "01", new GeoPoint(37.5, 127), "04");
        saveCharger("near", "02", new GeoPoint(37.5, 127), "02");
        saveCharger("far", "01", new GeoPoint(38.5, 128), "04");
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        StationSearchResult result = stationQueryService.search(
                new StationSearchQuery(new GeoPoint(37.5, 127), 1000, Connector.DC_COMBO, 20));

        assertThat(result.stations()).extracting(StationMatch::providerStationId).containsExactly("near");
        assertThat(result.stations().getFirst().chargers()).hasSize(1);
        assertThat(result.stations().getFirst().chargers().getFirst().chargerId()).isEqualTo("01");
        assertThat(result.stations().getFirst().distanceMeters()).isZero();
        assertThat(result.dataReady()).isFalse();
        assertThat(result.lastSuccessfulRunAt()).isNull();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        assertThat(statistics.getEntityLoadCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("북극을 포함하는 반경에서는 모든 경도의 가까운 후보를 유지한다")
    void preservesCandidatesAroundNorthPole() {
        saveCharger("north", "01", new GeoPoint(89.999, -120), "04");
        StationSearchResult result = stationQueryService.search(
                new StationSearchQuery(new GeoPoint(89.999, 60), 1000, Connector.DC_COMBO, 20));
        assertThat(result.stations()).extracting(StationMatch::providerStationId).containsExactly("north");
    }

    @Test
    @DisplayName("남극을 포함하는 반경에서도 경도와 무관하게 가까운 후보를 유지한다")
    void preservesCandidatesAroundSouthPole() {
        saveCharger("south", "01", new GeoPoint(-89.999, 120), "04");
        StationSearchResult result = stationQueryService.search(
                new StationSearchQuery(new GeoPoint(-89.999, -60), 1000, Connector.DC_COMBO, 20));
        assertThat(result.stations()).extracting(StationMatch::providerStationId).containsExactly("south");
    }

    private void saveCharger(String stationId, String chargerId, GeoPoint location, String connectorCode) {
        Instant collectedAt = Instant.parse("2026-10-05T00:00:00Z");
        ChargerDetails details = new ChargerDetails(connectorCode, "24시간", "N", null, null, null, null, null, null);
        stationUpsertService.upsert(new StationSnapshot(new ChargerId("ME", stationId, chargerId), "충전소", location,
                ChargerStatus.AVAILABLE, "2", details, null, collectedAt));
    }
}
