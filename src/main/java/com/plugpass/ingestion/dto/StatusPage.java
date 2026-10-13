package com.plugpass.ingestion.dto;

import java.util.List;

public record StatusPage(int pageNumber, int pageSize, long totalCount, List<ChargerStatusSnapshot> snapshots) {
    public StatusPage { snapshots = List.copyOf(snapshots); }
    public boolean hasNext() { return (long) pageNumber * pageSize < totalCount; }
}
