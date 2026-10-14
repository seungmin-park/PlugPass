package com.plugpass.recommendation.domain;
import java.util.List;
import com.plugpass.station.domain.ChargerStatus;
public record RankedCandidate(Long id, String name, double distanceMeters, List<String> reasonCodes, List<ChargerStatus> chargerStatuses) {
    public RankedCandidate { reasonCodes = List.copyOf(reasonCodes); chargerStatuses = List.copyOf(chargerStatuses); }
}
