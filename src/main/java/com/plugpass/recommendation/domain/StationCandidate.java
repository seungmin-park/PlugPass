package com.plugpass.recommendation.domain;
import java.util.List;
import com.plugpass.station.domain.Connector;
import com.plugpass.station.domain.ConnectorSelection;
import java.util.Set;
public record StationCandidate(Long id, String name, double distanceMeters, ConnectorSelection connectors, List<CandidateCharger> chargers) {
    public StationCandidate(Long id, String name, double distanceMeters, Connector connector, List<CandidateCharger> chargers) {
        this(id,name,distanceMeters,new ConnectorSelection(Set.of(connector)),chargers);
    }
    public StationCandidate { chargers = List.copyOf(chargers); }
}
