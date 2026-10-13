package com.plugpass.search.domain;

import com.plugpass.station.domain.Connector;
import java.util.Objects;

public record StationAreaSearchQuery(MapSearchArea area, Connector connector, int limit) {
    public StationAreaSearchQuery {
        Objects.requireNonNull(area, "area must not be null");
        Objects.requireNonNull(connector, "connector must not be null");
        if (limit < 1 || limit > 50) { throw new IllegalArgumentException("limit must be between 1 and 50"); }
    }
}
