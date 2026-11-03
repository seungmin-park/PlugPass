package com.plugpass.places.service;

import com.plugpass.exception.PlaceSearchException;
import com.plugpass.exception.PlaceSearchException.Failure;
import com.plugpass.places.client.PlaceSearchClient;
import com.plugpass.places.config.KakaoLocalProperties;
import com.plugpass.places.domain.PlaceSearchBudget;
import com.plugpass.places.dto.PlaceMatch;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
@Service
public class DefaultPlaceSearchService implements PlaceSearchService {
    private final PlaceSearchClient placeSearchClient;
    private final PlaceSearchBudget placeSearchBudget;
    private final Clock clock;
    public DefaultPlaceSearchService(PlaceSearchClient placeSearchClient, KakaoLocalProperties properties, Clock clock) {
        this.placeSearchClient = placeSearchClient;
        this.placeSearchBudget = new PlaceSearchBudget(properties.maxSearchesPerMinute());
        this.clock = clock;
    }
    @Override public List<PlaceMatch> search(String query) {
        if (query == null || query.strip().isEmpty() || query.strip().length() > 100) throw new IllegalArgumentException("place query must contain 1 to 100 characters");
        if (!placeSearchBudget.tryAcquire(clock.instant())) throw new PlaceSearchException(Failure.RATE_LIMIT);
        return placeSearchClient.search(query.strip());
    }
}
