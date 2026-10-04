package com.plugpass.search.dto.response;

import java.time.Instant;
import com.plugpass.search.dto.ChargerObservation;
import com.plugpass.station.domain.ChargerDetails;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.freshness.domain.Freshness;

public record ChargerResponse(String chargerId, ChargerStatus status, String rawStatus, Instant sourceObservedAt,
        Instant collectedAt, Freshness freshness, String reasonCode, String connectorCode, String useTime,
        String limitYn, String limitDetail, String note, String sourceStatusChangedAtRaw, String lastChargingStartedAtRaw,
        String lastChargingEndedAtRaw, String chargingStartedAtRaw, String delYn, String delDetail) {
    public static ChargerResponse from(ChargerObservation observation) {
        ChargerDetails details = observation.details();
        if (details == null) { details = new ChargerDetails(null,null,null,null,null,null,null,null,null); }
        return new ChargerResponse(observation.chargerId(),observation.status(),observation.rawStatus(),observation.sourceObservedAt(),
                observation.collectedAt(),observation.freshness().freshness(),observation.freshness().reasonCode(),
                details.connectorCode(),details.useTime(),details.limitYn(),details.limitDetail(),details.note(),
                details.sourceStatusChangedAtRaw(),details.lastChargingStartedAtRaw(),details.lastChargingEndedAtRaw(),
                details.chargingStartedAtRaw(),details.delYn(),details.delDetail());
    }
}
