package com.plugpass.recommendation;
import com.plugpass.search.StationSearchQuery;
public interface RecommendationService {
    CandidateGroups recommend(StationSearchQuery query, Long excludeStationId);
}
