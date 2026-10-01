package com.plugpass.ingestion;

import java.util.List;

public record StationPage(int pageNumber, int pageSize, long totalCount, List<StationSnapshot> snapshots) {
    public StationPage {
        snapshots = List.copyOf(snapshots);
    }
    public boolean hasNext() {
        return (long) pageNumber * pageSize < totalCount;
    }
}
