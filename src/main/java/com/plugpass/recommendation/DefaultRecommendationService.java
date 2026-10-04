package com.plugpass.recommendation;

import java.util.List;
import com.plugpass.search.ChargerObservation;
import com.plugpass.search.StationMatch;
import com.plugpass.search.StationQueryService;
import com.plugpass.search.StationSearchQuery;
import com.plugpass.station.ChargerDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultRecommendationService implements RecommendationService {
    private final StationQueryService stationQueryService;
    private final CandidatePolicy candidatePolicy = new CandidatePolicy();
    public DefaultRecommendationService(StationQueryService stationQueryService) { this.stationQueryService = stationQueryService; }
    @Override
    @Transactional(readOnly = true)
    public CandidateGroups recommend(StationSearchQuery query, Long excludeStationId) {
        List<StationCandidate> candidates = stationQueryService.searchWithinRadius(query).stations().stream()
                .filter(station -> !station.id().equals(excludeStationId)).map(station -> candidate(station,query)).toList();
        CandidateGroups groups = candidatePolicy.rank(candidates);
        return new CandidateGroups(groups.preferred().stream().limit(query.limit()).toList(),
                groups.requiresConfirmation().stream().limit(query.limit()).toList(),groups.excluded().stream().limit(query.limit()).toList());
    }
    private StationCandidate candidate(StationMatch station, StationSearchQuery query) {
        return new StationCandidate(station.id(),station.name(),station.distanceMeters(),query.connector(),
                station.chargers().stream().map(this::chargerCandidate).toList());
    }
    private CandidateCharger chargerCandidate(ChargerObservation charger) {
        ChargerDetails details = charger.details();
        return new CandidateCharger(details.connectorCode(),charger.status(),charger.freshness(),details.limitYn(),details.useTime(),details.delYn());
    }
}
