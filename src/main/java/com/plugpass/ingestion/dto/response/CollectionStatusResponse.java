package com.plugpass.ingestion.dto.response;

import java.time.Instant;
import com.plugpass.ingestion.dto.CollectionStatus;

public record CollectionStatusResponse(String state, Instant lastCatalogSuccessAt, Instant lastStatusSuccessAt,
        Instant lastAttemptAt, String lastAttemptStatus, int statusIntervalSeconds) {
    public static CollectionStatusResponse from(CollectionStatus status) {
        if (status == null) { return null; }
        return new CollectionStatusResponse(status.state(), status.lastCatalogSuccessAt(), status.lastStatusSuccessAt(),
                status.lastAttemptAt(), status.lastAttemptStatus(), status.statusIntervalSeconds());
    }
}
