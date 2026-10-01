package com.plugpass.ingestion;

import java.time.Instant;
import com.plugpass.station.ChargerDetails;
import com.plugpass.station.ChargerId;
import com.plugpass.station.ChargerStatus;
import com.plugpass.station.GeoPoint;

public record StationSnapshot(ChargerId chargerId, String stationName, GeoPoint location,
        ChargerStatus status, String rawStatus, ChargerDetails details, Instant sourceObservedAt, Instant collectedAt) {
    public StationSnapshot {
        if (chargerId == null) {
            throw new IllegalArgumentException("chargerId must not be null");
        }
        if (stationName == null || stationName.isBlank()) {
            throw new IllegalArgumentException("stationName must not be blank");
        }
        if (location == null) {
            throw new IllegalArgumentException("location must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (collectedAt == null) {
            throw new IllegalArgumentException("collectedAt must not be null");
        }
    }
}
