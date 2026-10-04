package com.plugpass.station.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public record ChargerDetails(String connectorCode, String useTime, String limitYn,
        String limitDetail, String note, String sourceStatusChangedAtRaw,
        String lastChargingStartedAtRaw, String lastChargingEndedAtRaw, String chargingStartedAtRaw,
        String delYn, String delDetail) {
    public ChargerDetails(String connectorCode, String useTime, String limitYn, String limitDetail,
            String note, String sourceStatusChangedAtRaw, String lastChargingStartedAtRaw,
            String lastChargingEndedAtRaw, String chargingStartedAtRaw) {
        this(connectorCode, useTime, limitYn, limitDetail, note, sourceStatusChangedAtRaw,
                lastChargingStartedAtRaw, lastChargingEndedAtRaw, chargingStartedAtRaw, null, null);
    }
}
