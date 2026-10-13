package com.plugpass.ingestion.service;

import com.plugpass.ingestion.dto.ChargerStatusSnapshot;
import com.plugpass.ingestion.dto.StationSnapshot;
import com.plugpass.station.domain.Charger;
import com.plugpass.station.domain.ChargerDetails;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.domain.GeoPoint;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.repository.StationRepository;
import java.time.Instant;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import java.util.stream.IntStream;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties="spring.jpa.properties.hibernate.generate_statistics=true")
class StatusUpsertTests {
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private StationUpsertService stationUpsertService;
    @Autowired private StatusUpsertService statusUpsertService;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private StationRepository stationRepository;
    @AfterEach void cleanOwnedData() {
        chargerRepository.deleteAllInBatch(); stationRepository.deleteAllInBatch();
    }
    @Test
    @DisplayName("상태 페이지도 충전기 수와 무관하게 한 번만 기존 충전기를 조회한다")
    void boundsStatusLookupPerPage() {
        Instant received = Instant.parse("2026-10-13T00:00:00Z");
        List<StationSnapshot> initial = IntStream.range(0,20).mapToObj(index -> new StationSnapshot(new ChargerId("ME","site-"+index,"01"),
                "충전소 "+index,new GeoPoint(37.5,127),ChargerStatus.AVAILABLE,"2",null,null,received)).toList();
        stationUpsertService.upsertPage(initial);
        List<ChargerStatusSnapshot> changes = initial.stream().map(snapshot -> new ChargerStatusSnapshot(snapshot.chargerId(),
                ChargerStatus.OCCUPIED,"3","20261013090000",null,null,null,received.plusSeconds(10))).toList();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        long before = statistics.getQueryExecutionCount();
        assertThat(statusUpsertService.applyPage(changes)).isEqualTo(20);
        assertThat(statistics.getQueryExecutionCount()-before).isLessThanOrEqualTo(1);
        assertThat(chargerRepository.findAll()).allSatisfy(charger -> assertThat(charger.getStatus()).isEqualTo(ChargerStatus.OCCUPIED));
    }
    @Test
    @DisplayName("상태 페이지 commit 후 별도 조회에서 위치·커넥터를 유지한 새 상태가 보인다")
    void commitsStateWithoutReplacingMetadata() {
        Instant received = Instant.parse("2026-10-13T00:00:00Z");
        ChargerId identity = new ChargerId("ME","001","01");
        stationUpsertService.upsert(new StationSnapshot(identity,"서울 충전소",new GeoPoint(37.5,127),ChargerStatus.AVAILABLE,"2",
                new ChargerDetails("04","24시간","N",null,null,"20261013080000",null,null,null),null,received));
        int matched = statusUpsertService.applyPage(List.of(new ChargerStatusSnapshot(identity,ChargerStatus.OCCUPIED,"3",
                "20261013090000",null,null,null,received.plusSeconds(10))));
        assertThat(matched).isEqualTo(1);
        Charger saved = chargerRepository.findByIdentity(identity).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ChargerStatus.OCCUPIED);
        assertThat(saved.getDetails().connectorCode()).isEqualTo("04");
        assertThat(saved.getCollectedAt()).isEqualTo(received.plusSeconds(10));
        assertThat(stationRepository.findAll()).singleElement().satisfies(station -> {
            assertThat(station.getName()).isEqualTo("서울 충전소");
            assertThat(station.getLocation()).isEqualTo(new GeoPoint(37.5,127));
        });
    }
    @Test
    @DisplayName("위치 없는 신규 상태로 충전소를 만들지 않으며 매칭되지 않은 건수를 구분한다")
    void doesNotInventMissingStation() {
        int matched = statusUpsertService.applyPage(List.of(new ChargerStatusSnapshot(new ChargerId("ME","missing","01"),
                ChargerStatus.OCCUPIED,"3","20261013090000",null,null,null,Instant.EPOCH)));
        assertThat(matched).isZero();
        assertThat(chargerRepository.findAll()).isEmpty();
        assertThat(stationRepository.findAll()).isEmpty();
    }
}
