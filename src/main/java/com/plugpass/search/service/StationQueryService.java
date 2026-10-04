package com.plugpass.search.service;

import com.plugpass.search.domain.StationSearchQuery;
import com.plugpass.search.dto.StationDetail;
import com.plugpass.search.dto.StationSearchResult;

public interface StationQueryService {
    StationSearchResult search(StationSearchQuery query);
    StationSearchResult searchWithinRadius(StationSearchQuery query);
    StationDetail detail(Long stationId);
}
