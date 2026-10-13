package com.plugpass.ingestion.domain;

import java.time.Instant;

/** Selects a recovery path when the provider's ten-minute delta window was missed. */
public final class CollectionSchedule {
    private CollectionSchedule() { }
    public static SyncKind select(Instant catalogCompletedAt, Instant latestCompletedAt, Instant now) {
        if (catalogCompletedAt == null || latestCompletedAt == null) { return SyncKind.CATALOG; }
        if (!catalogCompletedAt.plusSeconds(86400).isAfter(now)) { return SyncKind.CATALOG; }
        if (!latestCompletedAt.plusSeconds(600).isAfter(now)) { return SyncKind.CATALOG; }
        return SyncKind.STATUS;
    }
}
