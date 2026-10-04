package com.plugpass.ingestion.client;

import com.plugpass.station.domain.ChargerStatus;

public final class ProviderStatusMapper {
    public ChargerStatus map(String rawCode) {
        if (rawCode == null) {
            return ChargerStatus.UNKNOWN;
        }
        return switch (rawCode) {
            case "2" -> ChargerStatus.AVAILABLE;
            case "3" -> ChargerStatus.OCCUPIED;
            case "1", "4", "5" -> ChargerStatus.UNAVAILABLE;
            default -> ChargerStatus.UNKNOWN;
        };
    }
}
