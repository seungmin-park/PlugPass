package com.plugpass.search.domain;

import com.plugpass.station.domain.Connector;

import com.plugpass.station.domain.GeoPoint;

public record StationSearchQuery(GeoPoint location, int radiusMeters, Connector connector, int limit) {
    public StationSearchQuery {
        if (location == null) { throw new IllegalArgumentException("location must not be null"); }
        if (connector == null) { throw new IllegalArgumentException("connector must not be null"); }
        if (radiusMeters < 100 || radiusMeters > 10000) { throw new IllegalArgumentException("radiusMeters must be between 100 and 10000"); }
        if (limit < 1 || limit > 50) { throw new IllegalArgumentException("limit must be between 1 and 50"); }
    }
}
