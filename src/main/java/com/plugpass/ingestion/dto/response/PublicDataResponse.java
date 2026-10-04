package com.plugpass.ingestion.dto.response;

import java.util.List;

public record PublicDataResponse(int pageNumber, int pageSize, long totalCount, List<PublicDataItem> items) {
    public PublicDataResponse {
        items = List.copyOf(items);
    }
}
