package com.plugpass.recommendation.domain;
import java.util.List;
import com.plugpass.station.domain.Connector;
public record StationCandidate(Long id, String name, double distanceMeters, Connector connector, List<CandidateCharger> chargers) {
    public StationCandidate { chargers = List.copyOf(chargers); }
}
