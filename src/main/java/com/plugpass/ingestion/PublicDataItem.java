package com.plugpass.ingestion;

import java.time.Instant;
import com.plugpass.station.ChargerDetails;
import com.plugpass.station.ChargerId;
import com.plugpass.station.GeoPoint;

record PublicDataItem(String provider, String stationId, String chargerId, String stationName,
        String latitude, String longitude, String rawStatus, String connectorCode, String useTime,
        String limitYn, String limitDetail, String note, String sourceStatusChangedAtRaw,
        String lastChargingStartedAtRaw, String lastChargingEndedAtRaw, String chargingStartedAtRaw,
        String delYn, String delDetail) {
    StationSnapshot toSnapshot(ProviderStatusMapper statusMapper, Instant collectedAt) {
        ChargerDetails details = new ChargerDetails(connectorCode, useTime, limitYn, limitDetail, note,
                sourceStatusChangedAtRaw, lastChargingStartedAtRaw, lastChargingEndedAtRaw, chargingStartedAtRaw, delYn, delDetail);
        return new StationSnapshot(new ChargerId(provider, stationId, chargerId), stationName,
                new GeoPoint(Double.parseDouble(latitude), Double.parseDouble(longitude)),
                statusMapper.map(rawStatus), rawStatus, details, null, collectedAt);
    }
}
