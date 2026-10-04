package com.plugpass.search;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.plugpass.freshness.FreshnessPolicy;
import com.plugpass.exception.StationNotFoundException;
import com.plugpass.station.StationRepository;
import com.plugpass.ingestion.SyncRun;
import com.plugpass.ingestion.SyncRunRepository;
import com.plugpass.ingestion.SyncStatus;
import com.plugpass.station.Charger;
import com.plugpass.station.ChargerRepository;
import com.plugpass.station.Station;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultStationQueryService implements StationQueryService {
    private final ChargerRepository chargerRepository;
    private final SyncRunRepository syncRunRepository;
    private final FreshnessPolicy freshnessPolicy;
    private final Clock clock;
    private final StationRepository stationRepository;

    public DefaultStationQueryService(ChargerRepository chargerRepository, SyncRunRepository syncRunRepository,
            FreshnessPolicy freshnessPolicy, Clock clock, StationRepository stationRepository) {
        this.chargerRepository = chargerRepository;
        this.syncRunRepository = syncRunRepository;
        this.freshnessPolicy = freshnessPolicy;
        this.clock = clock;
        this.stationRepository = stationRepository;
    }
    @Override
    @Transactional(readOnly = true)
    public StationDetail detail(Long stationId) {
        if (stationId == null) { throw new IllegalArgumentException("stationId must not be null"); }
        Station station = stationRepository.findById(stationId).orElseThrow(StationNotFoundException::new);
        Instant now = clock.instant();
        List<ChargerObservation> chargers = chargerRepository.findByStationDatabaseId(stationId).stream()
                .sorted(Comparator.comparing(charger -> charger.getId().chargerId()))
                .map(charger -> observation(charger, now)).toList();
        return new StationDetail(station.getDatabaseId(),station.getProvider(),station.getStationId(),station.getName(),station.getLocation(),chargers);
    }

    @Override
    @Transactional(readOnly = true)
    public StationSearchResult search(StationSearchQuery query) {
        StationSearchResult result = searchWithinRadius(query);
        return new StationSearchResult(result.dataReady(),result.lastSuccessfulRunAt(),result.stations().stream().limit(query.limit()).toList());
    }
    @Override
    @Transactional(readOnly = true)
    public StationSearchResult searchWithinRadius(StationSearchQuery query) {
        Instant now = clock.instant();
        Instant lastSuccessfulRunAt = syncRunRepository.findFirstByStatusOrderByCompletedAtDesc(SyncStatus.SUCCESS)
                .map(SyncRun::getCompletedAt).orElse(null);
        Map<Station,List<Charger>> byStation = chargerRepository.findAllWithStation().stream()
                .filter(charger -> charger.getDetails() != null && query.connector().matches(charger.getDetails().connectorCode()))
                .collect(Collectors.groupingBy(Charger::getStation));
        List<StationMatch> matches = byStation.entrySet().stream()
                .map(entry -> match(entry.getKey(), entry.getValue(), query, now))
                .filter(station -> station.distanceMeters() <= query.radiusMeters())
                .sorted(Comparator.comparingDouble(StationMatch::distanceMeters).thenComparing(StationMatch::id))
                .toList();
        return new StationSearchResult(lastSuccessfulRunAt != null, lastSuccessfulRunAt, matches);
    }
    private StationMatch match(Station station, List<Charger> chargers, StationSearchQuery query, Instant now) {
        List<ChargerObservation> observations = chargers.stream().sorted(Comparator.comparing(charger -> charger.getId().chargerId()))
                .map(charger -> observation(charger, now)).toList();
        return new StationMatch(station.getDatabaseId(),station.getProvider(),station.getStationId(),station.getName(),
                station.getLocation(),query.location().distanceMetersTo(station.getLocation()),observations);
    }
    private ChargerObservation observation(Charger charger, Instant now) {
        return new ChargerObservation(charger.getId().chargerId(),charger.getStatus(),charger.getRawStatus(),
                charger.getSourceObservedAt(),charger.getCollectedAt(),freshnessPolicy.assess(charger.getSourceObservedAt(),now),charger.getDetails());
    }
}
