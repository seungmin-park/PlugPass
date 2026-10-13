package com.plugpass.search.dto.request;

import com.plugpass.common.validation.FiniteDouble;
import com.plugpass.search.domain.MapSearchArea;
import com.plugpass.search.domain.StationAreaSearchQuery;
import com.plugpass.station.domain.Connector;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public record StationAreaSearchRequest(
        @NotNull(message = "남쪽 경계는 필수입니다") @FiniteDouble Double south,
        @NotNull(message = "북쪽 경계는 필수입니다") @FiniteDouble Double north,
        @NotNull(message = "서쪽 경계는 필수입니다") @FiniteDouble Double west,
        @NotNull(message = "동쪽 경계는 필수입니다") @FiniteDouble Double east,
        @NotNull(message = "커넥터는 필수입니다") Connector connector,
        @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 50, message = "limit은 50 이하여야 합니다") Integer limit) {
    public StationAreaSearchRequest { if (limit == null) { limit = 20; } }
    @AssertTrue(message = "검색 영역을 확인하고 지도를 확대해 주세요")
    public boolean isAreaValid() {
        if (south == null || north == null || west == null || east == null) { return true; }
        try { new MapSearchArea(south, north, west, east); return true; }
        catch (IllegalArgumentException exception) { return false; }
    }
    public StationAreaSearchQuery toQuery() {
        return new StationAreaSearchQuery(new MapSearchArea(south, north, west, east), connector, limit);
    }
}
