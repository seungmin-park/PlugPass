package com.plugpass.ingestion.dto.response;

import com.plugpass.ingestion.dto.ChargerStatusSnapshot;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.domain.ChargerStatus;
import java.time.Instant;

public record PublicStatusItem(String provider, String stationId, String chargerId, String rawStatus,
        String changedAtRaw, String lastStartedAtRaw, String lastEndedAtRaw, String startedAtRaw) {
    public ChargerStatusSnapshot toSnapshot(ChargerStatus status, Instant collectedAt) {
        return new ChargerStatusSnapshot(new ChargerId(provider, stationId, chargerId), status, rawStatus,
                changedAtRaw, lastStartedAtRaw, lastEndedAtRaw, startedAtRaw, collectedAt);
    }
}
