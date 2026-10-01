package com.plugpass.station;

public record Station(String provider, String stationId, String name, GeoPoint location) {

    public Station {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        if (stationId == null || stationId.isBlank()) {
            throw new IllegalArgumentException("stationId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (location == null) {
            throw new IllegalArgumentException("location must not be null");
        }
    }
}
