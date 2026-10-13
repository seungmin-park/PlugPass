package com.plugpass.search.domain;

import com.plugpass.station.domain.GeoPoint;

public record MapSearchArea(double south, double north, double west, double east) {
    public MapSearchArea {
        GeoPoint southwest = new GeoPoint(south, west);
        GeoPoint northeast = new GeoPoint(north, east);
        if (south >= north || west >= east || north - south > 1 || east - west > 1) {
            throw new IllegalArgumentException("map area must be ordered and at most one degree wide");
        }
        GeoPoint center = new GeoPoint((south + north) / 2, (west + east) / 2);
        if (Math.max(center.distanceMetersTo(southwest), center.distanceMetersTo(northeast)) > 10000) {
            throw new IllegalArgumentException("map area corners must be within 10000 meters of its center");
        }
    }
    public GeoPoint center() { return new GeoPoint((south + north) / 2, (west + east) / 2); }
}
