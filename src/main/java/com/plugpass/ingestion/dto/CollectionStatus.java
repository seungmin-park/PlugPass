package com.plugpass.ingestion.dto;

import java.time.Instant;

public record CollectionStatus(String state, Instant lastCatalogSuccessAt, Instant lastStatusSuccessAt,
        Instant lastAttemptAt, String lastAttemptStatus, int statusIntervalSeconds) { }
