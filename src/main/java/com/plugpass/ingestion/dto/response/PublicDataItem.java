package com.plugpass.ingestion.dto.response;

import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.ingestion.dto.StationSnapshot;

import java.time.Instant;
import com.plugpass.station.domain.ChargerDetails;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.domain.GeoPoint;

public record PublicDataItem(String provider, String stationId, String chargerId, String stationName,
        String latitude, String longitude, String rawStatus, String connectorCode, String useTime,
        String limitYn, String limitDetail, String note, String sourceStatusChangedAtRaw,
        String lastChargingStartedAtRaw, String lastChargingEndedAtRaw, String chargingStartedAtRaw,
        String delYn, String delDetail) {
    public StationSnapshot toSnapshot(ChargerStatus status, Instant collectedAt) {
        ChargerDetails details = new ChargerDetails(connectorCode, useTime, limitYn, limitDetail, note,
                sourceStatusChangedAtRaw, lastChargingStartedAtRaw, lastChargingEndedAtRaw, chargingStartedAtRaw, delYn, delDetail);
        return new StationSnapshot(new ChargerId(provider, stationId, chargerId), stationName,
                new GeoPoint(Double.parseDouble(latitude), Double.parseDouble(longitude)),
                status, rawStatus, details, null, collectedAt);
    }
}
