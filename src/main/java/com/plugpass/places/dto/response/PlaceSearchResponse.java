package com.plugpass.places.dto.response;

import com.plugpass.places.dto.PlaceMatch;
import java.util.List;
public record PlaceSearchResponse(List<PlaceMatch> places) {
    public PlaceSearchResponse { places = List.copyOf(places); }
}
