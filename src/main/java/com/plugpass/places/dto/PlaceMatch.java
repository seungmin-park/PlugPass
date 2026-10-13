package com.plugpass.places.dto;

import com.plugpass.station.domain.GeoPoint;
public record PlaceMatch(String name, String address, double latitude, double longitude) {
    public PlaceMatch {
        if (name == null || name.isBlank() || address == null) throw new IllegalArgumentException("place name and address are required");
        new GeoPoint(latitude, longitude);
    }
}
