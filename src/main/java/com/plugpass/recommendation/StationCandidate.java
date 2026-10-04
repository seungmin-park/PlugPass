package com.plugpass.recommendation;
import java.util.List;
import com.plugpass.search.Connector;
public record StationCandidate(Long id, String name, double distanceMeters, Connector connector, List<CandidateCharger> chargers) {
    public StationCandidate { chargers = List.copyOf(chargers); }
}
