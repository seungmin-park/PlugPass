package com.plugpass.search.dto;
import java.time.Instant;
import java.util.List;
public record StationSearchResult(boolean dataReady, Instant lastSuccessfulRunAt, List<StationMatch> stations) {
    public StationSearchResult { stations = List.copyOf(stations); }
}
