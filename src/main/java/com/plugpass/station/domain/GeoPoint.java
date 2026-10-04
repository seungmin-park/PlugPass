package com.plugpass.station.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;

@Embeddable
public record GeoPoint(@Column(nullable = false) double latitude, @Column(nullable = false) double longitude) {

    public double distanceMetersTo(GeoPoint destination) {
        double latitudeDelta = Math.toRadians(destination.latitude - latitude);
        double longitudeDelta = Math.toRadians(destination.longitude - longitude);
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(destination.latitude))
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        return 2 * 6371008.8 * Math.asin(Math.sqrt(Math.clamp(haversine, 0, 1)));
    }

    public GeoPoint {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude must be finite and between -90 and 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude must be finite and between -180 and 180");
        }
    }
}
