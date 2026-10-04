package com.plugpass.search.dto.request;

import com.plugpass.common.validation.FiniteDouble;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import com.plugpass.station.domain.Connector;
import com.plugpass.search.domain.StationSearchQuery;
import com.plugpass.station.domain.GeoPoint;

public record StationSearchRequest(
        @NotNull(message = "위도는 필수입니다") @FiniteDouble
        @DecimalMin(value = "-90",message = "위도는 -90 이상이어야 합니다") @DecimalMax(value = "90",message = "위도는 90 이하여야 합니다") Double latitude,
        @NotNull(message = "경도는 필수입니다") @FiniteDouble
        @DecimalMin(value = "-180",message = "경도는 -180 이상이어야 합니다") @DecimalMax(value = "180",message = "경도는 180 이하여야 합니다") Double longitude,
        @NotNull(message = "반경은 필수입니다") @Min(value = 100,message = "반경은 100m 이상이어야 합니다") @Max(value = 10000,message = "반경은 10000m 이하여야 합니다") Integer radiusMeters,
        @NotNull(message = "커넥터는 필수입니다") Connector connector,
        @Min(value = 1,message = "limit은 1 이상이어야 합니다") @Max(value = 50,message = "limit은 50 이하여야 합니다") Integer limit) {
    public StationSearchRequest { if (limit == null) { limit = 20; } }
    public StationSearchQuery toQuery() { return new StationSearchQuery(new GeoPoint(latitude,longitude),radiusMeters,connector,limit); }
}
