package com.plugpass.recommendation.service;

import com.plugpass.recommendation.domain.CandidateGroups;
import com.plugpass.search.domain.StationSearchQuery;
public interface RecommendationService {
    CandidateGroups recommend(StationSearchQuery query, Long excludeStationId);
}
