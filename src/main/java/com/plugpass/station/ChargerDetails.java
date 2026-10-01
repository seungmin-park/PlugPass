package com.plugpass.station;

import jakarta.persistence.Embeddable;

@Embeddable
public record ChargerDetails(String connectorCode, String useTime, String limitYn,
        String limitDetail, String note, String sourceStatusChangedAtRaw,
        String lastChargingStartedAtRaw, String lastChargingEndedAtRaw, String chargingStartedAtRaw) {
}
