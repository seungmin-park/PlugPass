package performance;

import com.plugpass.PlugPassApplication;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.station.domain.Charger;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.repository.StationRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PlugPassApplication.class, properties = "spring.datasource.url=jdbc:h2:mem:performance-dataset-test")
class PerformanceDatasetTests {
    @Autowired private StationRepository stationRepository;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private SyncRunRepository syncRunRepository;

    @AfterEach
    void deleteOwnedData() {
        chargerRepository.deleteAllInBatch();
        stationRepository.deleteAllInBatch();
        syncRunRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("성능 생성 데이터는 충전소당5개·고정 위치·최신성4종과 실제 매핑을 유지한다")
    void seedsDeterministicMappedData() throws Exception {
        new PerformanceDataset(stationRepository, chargerRepository, syncRunRepository).seed(16);
        assertThat(stationRepository.count()).isEqualTo(16);
        assertThat(chargerRepository.count()).isEqualTo(80);
        List<Charger> firstChargers = chargerRepository.findByStationDatabaseId(1L);
        assertThat(firstChargers).hasSize(5).allSatisfy(charger -> {
            assertThat(charger.getStatus()).isEqualTo(ChargerStatus.AVAILABLE);
            assertThat(charger.getSourceObservedAt()).isEqualTo(Instant.parse(PerformanceDataset.OBSERVED_AT));
            assertThat(charger.getCreatedAt()).isEqualTo(charger.getUpdatedAt());
            assertThat(charger.getDetails().limitYn()).isEqualTo("N");
        });
        assertThat(firstChargers.stream().map(charger -> charger.getDetails().connectorCode()).toList())
                .containsExactlyInAnyOrder("04", "04", "04", "04", "02");
        assertThat(chargerRepository.findByStationDatabaseId(2L)).allSatisfy(charger -> assertThat(charger.getSourceObservedAt()).isNull());
        assertThat(chargerRepository.findByStationDatabaseId(3L)).allSatisfy(charger ->
                assertThat(charger.getSourceObservedAt()).isEqualTo(Instant.parse(PerformanceDataset.OBSERVED_AT).minusSeconds(601)));
        assertThat(chargerRepository.findByStationDatabaseId(4L)).allSatisfy(charger -> assertThat(charger.getStatus()).isEqualTo(ChargerStatus.OCCUPIED));
        assertThat(syncRunRepository.findFirstByStatusOrderByCompletedAtDesc(SyncStatus.SUCCESS).orElseThrow().getProcessedCount()).isEqualTo(80);
    }
}
