package com.plugpass.ingestion.service;

import com.plugpass.ingestion.dto.StationSnapshot;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.domain.GeoPoint;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.repository.StationRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class StationPageEfficiencyTests {
    @Autowired private StationUpsertService stationUpsertService;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private StationRepository stationRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @AfterEach void cleanOwnedData() { chargerRepository.deleteAllInBatch(); stationRepository.deleteAllInBatch(); }
    @Test
    @DisplayName("페이지 내 충전기 수가 늘어도 기존 데이터 조회는 충전소·충전기 각 한번으로 제한한다")
    void boundsReadQueriesPerPage() {
        Instant received = Instant.parse("2026-10-13T00:00:00Z");
        List<StationSnapshot> snapshots = IntStream.range(0,20).mapToObj(index -> new StationSnapshot(
                new ChargerId("ME","station-"+index,"01"),"충전소 "+index,new GeoPoint(37.5,127),
                ChargerStatus.AVAILABLE,"2",null,null,received)).toList();
        stationUpsertService.upsertPage(snapshots);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        long queriesBefore = statistics.getQueryExecutionCount();
        stationUpsertService.upsertPage(snapshots);
        assertThat(statistics.getQueryExecutionCount()-queriesBefore).isLessThanOrEqualTo(2);
        assertThat(chargerRepository.count()).isEqualTo(20);
        assertThat(stationRepository.count()).isEqualTo(20);
    }
}
