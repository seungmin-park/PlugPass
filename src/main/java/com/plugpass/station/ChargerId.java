package com.plugpass.station;

public record ChargerId(String provider, String stationId, String chargerId) {

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
