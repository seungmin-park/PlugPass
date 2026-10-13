package com.plugpass.places.service;

import com.plugpass.places.client.PlaceSearchClient;
import com.plugpass.places.dto.PlaceMatch;
import java.util.List;
import org.springframework.stereotype.Service;
@Service
public class DefaultPlaceSearchService implements PlaceSearchService {
    private final PlaceSearchClient placeSearchClient;
    public DefaultPlaceSearchService(PlaceSearchClient placeSearchClient) { this.placeSearchClient = placeSearchClient; }
    @Override public List<PlaceMatch> search(String query) {
        if (query == null || query.strip().isEmpty() || query.strip().length() > 100) throw new IllegalArgumentException("place query must contain 1 to 100 characters");
        return placeSearchClient.search(query.strip());
    }
}
