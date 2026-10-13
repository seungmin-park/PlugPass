package com.plugpass.ingestion.domain;

import java.time.Instant;

public enum CollectionHealth {
    NOT_READY, CURRENT, DELAYED;
    public static CollectionHealth assess(Instant catalog, Instant latestSuccess, SyncRun latestAttempt, Instant now) {
        if (catalog == null) { return NOT_READY; }
        if (CollectionSchedule.select(catalog, latestSuccess, now) == SyncKind.CATALOG) { return DELAYED; }
        if (latestAttempt != null && latestAttempt.getCompletedAt() != null
                && latestAttempt.getCompletedAt().isAfter(latestSuccess)
                && latestAttempt.getStatus() != SyncStatus.SUCCESS) { return DELAYED; }
        return CURRENT;
    }
}
