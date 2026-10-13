package com.plugpass.ingestion.dto;

import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.domain.ChargerStatusChange;
import java.time.Instant;

public record ChargerStatusSnapshot(ChargerId chargerId, ChargerStatus status, String rawStatus,
        String changedAtRaw, String lastStartedAtRaw, String lastEndedAtRaw, String startedAtRaw, Instant collectedAt) {
    public ChargerStatusSnapshot {
        if (chargerId == null) { throw new IllegalArgumentException("charger identity is required"); }
        new ChargerStatusChange(status,rawStatus,changedAtRaw,lastStartedAtRaw,lastEndedAtRaw,startedAtRaw,collectedAt);
    }
    public ChargerStatusChange change() {
        return new ChargerStatusChange(status,rawStatus,changedAtRaw,lastStartedAtRaw,lastEndedAtRaw,startedAtRaw,collectedAt);
    }
}
