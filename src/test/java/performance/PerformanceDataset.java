package performance;

import com.plugpass.ingestion.domain.SyncRun;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.station.domain.Charger;
import com.plugpass.station.domain.ChargerDetails;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.domain.GeoPoint;
import com.plugpass.station.domain.Station;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.repository.StationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Generated read-only measurement fixture; never included in the application JAR. */
public final class PerformanceDataset {
    public static final String OBSERVED_AT = "2026-10-05T00:00:00Z";
    private final StationRepository stationRepository;
    private final ChargerRepository chargerRepository;
    private final SyncRunRepository syncRunRepository;
    public PerformanceDataset(StationRepository stationRepository, ChargerRepository chargerRepository, SyncRunRepository syncRunRepository) {
        this.stationRepository = stationRepository;
        this.chargerRepository = chargerRepository;
        this.syncRunRepository = syncRunRepository;
    }
    public void seed(int stationCount) {
        if (stationCount < 1 || stationCount > 10000) { throw new IllegalArgumentException("stationCount must be 1..10000"); }
        Instant now = Instant.parse(OBSERVED_AT);
        List<Station> stations = new ArrayList<>();
        for (int index = 0; index < stationCount; index++) {
            stations.add(Station.builder().provider("PERF").stationId(Integer.toString(index)).name("generated-" + index)
                    .location(new GeoPoint(37.5 + (index / 100) * .001, 127 + (index % 100) * .001)).createdAt(now).build());
        }
        stationRepository.saveAll(stations);
        List<Charger> chargers = new ArrayList<>();
        for (int index = 0; index < stationCount * 5; index++) {
            int stationIndex = index / 5;
            int chargerNumber = index % 5 + 1;
            int category = stationIndex % 4;
            Station station = stations.get(stationIndex);
            chargers.add(Charger.builder().id(new ChargerId("PERF", station.getStationId(), String.format("%02d", chargerNumber)))
                    .station(station).status(category == 3 ? ChargerStatus.OCCUPIED : ChargerStatus.AVAILABLE)
                    .rawStatus(category == 3 ? "3" : "2")
                    .details(new ChargerDetails(chargerNumber == 5 ? "02" : "04", "24시간", "N", null, null, null, null, null, null, "N", null))
                    .sourceObservedAt(category == 1 ? null : category == 2 ? now.minusSeconds(601) : now).collectedAt(now).build());
        }
        chargerRepository.saveAll(chargers);
        SyncRun run = SyncRun.builder().startedAt(now).build();
        run.complete(stationCount * 5L, null, null, now);
        syncRunRepository.save(run);
    }
}
