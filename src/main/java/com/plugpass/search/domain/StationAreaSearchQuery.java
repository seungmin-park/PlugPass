package com.plugpass.search.domain;

import com.plugpass.station.domain.Connector;
import com.plugpass.station.domain.ConnectorSelection;
import java.util.Objects;
import java.util.Set;

public record StationAreaSearchQuery(MapSearchArea area, ConnectorSelection connectors, int limit) {
    public StationAreaSearchQuery(MapSearchArea area, Connector connector, int limit) {
        this(area, connector == null ? null : new ConnectorSelection(Set.of(connector)), limit);
    }
    public StationAreaSearchQuery {
        Objects.requireNonNull(area, "area must not be null");
        Objects.requireNonNull(connectors, "connector must not be null");
        if (limit < 1 || limit > 50) { throw new IllegalArgumentException("limit must be between 1 and 50"); }
    }
}
