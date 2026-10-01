package com.plugpass.ingestion;

import java.util.List;

record PublicDataResponse(int pageNumber, int pageSize, long totalCount, List<PublicDataItem> items) {
    PublicDataResponse {
        items = List.copyOf(items);
    }
}
