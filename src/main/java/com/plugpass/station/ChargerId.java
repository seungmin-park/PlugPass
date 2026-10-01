package com.plugpass.station;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;

@Embeddable
public record ChargerId(@Column(nullable = false) String provider, @Column(name = "station_id", nullable = false) String stationId, @Column(name = "charger_number", nullable = false) String chargerId) {

    public ChargerId {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        if (stationId == null || stationId.isBlank()) {
            throw new IllegalArgumentException("stationId must not be blank");
        }
        if (chargerId == null || chargerId.isBlank()) {
            throw new IllegalArgumentException("chargerId must not be blank");
        }
    }
}
