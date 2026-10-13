package com.plugpass.search.domain;

import com.plugpass.station.domain.Connector;
import com.plugpass.station.domain.ConnectorSelection;
import java.util.Set;

import com.plugpass.station.domain.GeoPoint;

public record StationSearchQuery(GeoPoint location, int radiusMeters, ConnectorSelection connectors, int limit) {
    public StationSearchQuery(GeoPoint location, int radiusMeters, Connector connector, int limit) {
        this(location, radiusMeters, connector == null ? null : new ConnectorSelection(Set.of(connector)), limit);
    }
    public StationSearchQuery {
        if (location == null) { throw new IllegalArgumentException("location must not be null"); }
        if (connectors == null) { throw new IllegalArgumentException("connector must not be null"); }
        if (radiusMeters < 100 || radiusMeters > 10000) { throw new IllegalArgumentException("radiusMeters must be between 100 and 10000"); }
        if (limit < 1 || limit > 50) { throw new IllegalArgumentException("limit must be between 1 and 50"); }
    }
}
