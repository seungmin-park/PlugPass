package com.plugpass.search.response;

import java.time.Instant;
import java.util.List;
import com.plugpass.search.StationSearchResult;
import com.plugpass.search.StationMatch;
import com.plugpass.station.ChargerStatus;

public record StationSearchResponse(boolean dataReady, Instant lastSuccessfulRunAt, List<StationSummaryResponse> stations) {
    public static StationSearchResponse from(StationSearchResult result) {
        return new StationSearchResponse(result.dataReady(),result.lastSuccessfulRunAt(),result.stations().stream().map(StationSummaryResponse::from).toList());
    }
    public record StationSummaryResponse(Long id, String provider, String providerStationId, String name,
            double latitude, double longitude, double distanceMeters, int compatibleChargerCount, long reportedAvailableCount,
            List<ChargerResponse> chargers) {
        static StationSummaryResponse from(StationMatch station) {
            List<ChargerResponse> chargers = station.chargers().stream().map(ChargerResponse::from).toList();
            return new StationSummaryResponse(station.id(),station.provider(),station.providerStationId(),station.name(),
                    station.location().latitude(),station.location().longitude(),station.distanceMeters(),chargers.size(),
                    chargers.stream().filter(charger -> charger.status() == ChargerStatus.AVAILABLE).count(),chargers);
        }
    }
}
