package com.plugpass.search.domain;

import com.plugpass.station.domain.GeoPoint;

/** Conservative spherical bounding box; exact distance remains the final filter. */
public record SearchBounds(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
    public static SearchBounds around(GeoPoint center, int radiusMeters) {
        double angularRadius = radiusMeters / 6371008.8;
        double latitudeDelta = Math.toDegrees(angularRadius) + 1e-9;
        double minLatitude = Math.max(-90, center.latitude() - latitudeDelta);
        double maxLatitude = Math.min(90, center.latitude() + latitudeDelta);
        if (minLatitude == -90 || maxLatitude == 90) {
            return new SearchBounds(minLatitude, maxLatitude, -180, 180);
        }
        double longitudeDelta = Math.toDegrees(Math.asin(Math.sin(angularRadius)
                / Math.cos(Math.toRadians(center.latitude())))) + 1e-9;
        return new SearchBounds(minLatitude, maxLatitude,
                normalizeLongitude(center.longitude() - longitudeDelta),
                normalizeLongitude(center.longitude() + longitudeDelta));
    }

    private static double normalizeLongitude(double longitude) {
        if (longitude < -180) { return longitude + 360; }
        if (longitude > 180) { return longitude - 360; }
        return longitude;
    }
}
