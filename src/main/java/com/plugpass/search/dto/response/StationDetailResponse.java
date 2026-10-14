package com.plugpass.search.dto.response;

import java.util.List;
import com.plugpass.ingestion.dto.CollectionStatus;
import com.plugpass.ingestion.dto.response.CollectionStatusResponse;
import com.plugpass.search.dto.StationDetail;

public record StationDetailResponse(Long id, String provider, String providerStationId, String name,
        double latitude, double longitude, List<ChargerResponse> chargers, CollectionStatusResponse collection) {
    public static StationDetailResponse from(StationDetail detail, CollectionStatus collection) {
        return new StationDetailResponse(detail.id(),detail.provider(),detail.providerStationId(),detail.name(),
                detail.location().latitude(),detail.location().longitude(),detail.chargers().stream().map(ChargerResponse::from).toList(), CollectionStatusResponse.from(collection));
    }
}
