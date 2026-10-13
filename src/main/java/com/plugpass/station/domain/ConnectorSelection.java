package com.plugpass.station.domain;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record ConnectorSelection(Set<Connector> connectors) {
    public ConnectorSelection {
        if (connectors == null || connectors.isEmpty() || connectors.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("connectors must contain at least one known connector");
        }
        connectors = Set.copyOf(connectors);
    }

    public static ConnectorSelection from(Connector connector, List<Connector> connectors) {
        if (connector != null && connectors != null) {
            throw new IllegalArgumentException("connector and connectors cannot be combined");
        }
        if (connector != null) { return new ConnectorSelection(Set.of(connector)); }
        if (connectors == null) { throw new IllegalArgumentException("connectors must not be empty"); }
        return new ConnectorSelection(Set.copyOf(connectors));
    }

    public Set<String> providerCodes() {
        return connectors.stream().flatMap(connector -> connector.providerCodes().stream()).collect(Collectors.toUnmodifiableSet());
    }

    public boolean matches(String providerCode) {
        return connectors.stream().anyMatch(connector -> connector.matches(providerCode));
    }
}
