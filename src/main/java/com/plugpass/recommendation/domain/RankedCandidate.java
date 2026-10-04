package com.plugpass.recommendation.domain;
import java.util.List;
public record RankedCandidate(Long id, String name, double distanceMeters, List<String> reasonCodes) {
    public RankedCandidate { reasonCodes = List.copyOf(reasonCodes); }
}
