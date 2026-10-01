package com.plugpass.station;

public record Charger(ChargerId id, ChargerStatus status, String rawStatus) {
    public Charger(ChargerId id) {
        this(id, ChargerStatus.UNKNOWN, null);
    }
    public Charger {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
    }
}
