package com.plugpass.search;

public interface StationQueryService {
    StationSearchResult search(StationSearchQuery query);
    StationDetail detail(Long stationId);
}
