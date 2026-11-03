package com.plugpass.search.service;

import com.plugpass.search.domain.StationSearchQuery;
import com.plugpass.search.domain.StationAreaSearchQuery;
import com.plugpass.search.domain.MapSearchArea;
import com.plugpass.station.domain.GeoPoint;
import com.plugpass.search.domain.SearchBounds;
import com.plugpass.search.dto.ChargerObservation;
import com.plugpass.search.dto.StationDetail;
import com.plugpass.search.dto.StationMatch;
import com.plugpass.search.dto.StationSearchResult;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.plugpass.freshness.domain.FreshnessPolicy;
import com.plugpass.exception.StationNotFoundException;
import com.plugpass.station.repository.StationRepository;
import com.plugpass.ingestion.domain.SyncRun;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.domain.SyncKind;
import com.plugpass.station.domain.Charger;
import com.plugpass.station.repository.CandidateStationLocation;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.domain.Station;
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
        Instant now = clock.instant();
        Instant lastSuccessfulRunAt = lastCatalogSuccess();
        SearchBounds bounds = SearchBounds.around(query.location(), query.radiusMeters());
        List<Long> nearest = rankCandidates(chargerRepository.findCandidateStationLocations(
                bounds.minLatitude(), bounds.maxLatitude(), bounds.minLongitude(), bounds.maxLongitude(),
                query.connectors().providerCodes()), query.location(), query.radiusMeters(), query.limit());
        return new StationSearchResult(lastSuccessfulRunAt != null, lastSuccessfulRunAt,
                matchesAt(nearest, query.connectors().providerCodes(), query.location(), now));
    }
    @Override
    @Transactional(readOnly = true)
    public StationSearchResult searchWithinRadius(StationSearchQuery query) {
        Instant now = clock.instant();
        Instant lastSuccessfulRunAt = lastCatalogSuccess();
        SearchBounds bounds = SearchBounds.around(query.location(), query.radiusMeters());
        Map<Station,List<Charger>> byStation = chargerRepository.findSearchCandidates(
                bounds.minLatitude(), bounds.maxLatitude(), bounds.minLongitude(), bounds.maxLongitude(),
                query.connectors().providerCodes()).stream()
                .collect(Collectors.groupingBy(Charger::getStation));
        List<StationMatch> matches = byStation.entrySet().stream()
                .map(entry -> match(entry.getKey(), entry.getValue(), query.location(), now))
                .filter(station -> station.distanceMeters() <= query.radiusMeters())
                .sorted(Comparator.comparingDouble(StationMatch::distanceMeters).thenComparing(StationMatch::id))
                .toList();
        return new StationSearchResult(lastSuccessfulRunAt != null, lastSuccessfulRunAt, matches);
    }
    @Override
    @Transactional(readOnly = true)
    public StationSearchResult searchArea(StationAreaSearchQuery query) {
        Instant now = clock.instant();
        Instant lastSuccessfulRunAt = lastCatalogSuccess();
        MapSearchArea area = query.area();
        List<Long> nearest = rankCandidates(chargerRepository.findCandidateStationLocations(
                area.south(), area.north(), area.west(), area.east(), query.connectors().providerCodes()),
                area.center(), Double.POSITIVE_INFINITY, query.limit());
        return new StationSearchResult(lastSuccessfulRunAt != null, lastSuccessfulRunAt,
                matchesAt(nearest, query.connectors().providerCodes(), area.center(), now));
    }
    /** Ranks lightweight candidates by exact distance so chargers are loaded only for stations that are returned. */
    private List<Long> rankCandidates(List<CandidateStationLocation> candidates, GeoPoint center, double maxDistanceMeters, int limit) {
        return candidates.stream()
                .map(candidate -> new StationDistance(candidate.getStationId(),
                        center.distanceMetersTo(new GeoPoint(candidate.getLatitude(), candidate.getLongitude()))))
                .filter(candidate -> candidate.distanceMeters() <= maxDistanceMeters)
                .sorted(Comparator.comparingDouble(StationDistance::distanceMeters).thenComparing(StationDistance::stationId))
                .limit(limit).map(StationDistance::stationId).toList();
    }
    private List<StationMatch> matchesAt(List<Long> stationIds, Set<String> connectorCodes, GeoPoint center, Instant now) {
        if (stationIds.isEmpty()) { return List.of(); }
        return chargerRepository.findCompatibleChargersAt(stationIds, connectorCodes).stream()
                .collect(Collectors.groupingBy(Charger::getStation)).entrySet().stream()
                .map(entry -> match(entry.getKey(), entry.getValue(), center, now))
                .sorted(Comparator.comparingDouble(StationMatch::distanceMeters).thenComparing(StationMatch::id))
                .toList();
    }
    private Instant lastCatalogSuccess() {
        return syncRunRepository.findFirstByKindAndStatusOrderByCompletedAtDesc(SyncKind.CATALOG, SyncStatus.SUCCESS)
                .map(SyncRun::getCompletedAt).orElse(null);
    }
    private record StationDistance(Long stationId, double distanceMeters) { }
    private StationMatch match(Station station, List<Charger> chargers, GeoPoint center, Instant now) {
        List<ChargerObservation> observations = chargers.stream().sorted(Comparator.comparing(charger -> charger.getId().chargerId()))
                .map(charger -> observation(charger, now)).toList();
        return new StationMatch(station.getDatabaseId(),station.getProvider(),station.getStationId(),station.getName(),
                station.getLocation(),center.distanceMetersTo(station.getLocation()),observations);
    }
    private ChargerObservation observation(Charger charger, Instant now) {
        return new ChargerObservation(charger.getId().chargerId(),charger.getStatus(),charger.getRawStatus(),
                charger.getSourceObservedAt(),charger.getCollectedAt(),freshnessPolicy.assess(charger.getSourceObservedAt(),now),charger.getDetails());
    }
}
