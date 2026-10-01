package com.plugpass.station;

public record Charger(ChargerId id) {

    public Charger {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
    }
}
