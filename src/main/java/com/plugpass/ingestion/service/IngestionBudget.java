package com.plugpass.ingestion.service;

import com.plugpass.ingestion.config.IngestionProperties;
import com.plugpass.ingestion.client.IngestionTime;

import java.time.Duration;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;

final class IngestionBudget {
    private final IngestionProperties properties;
    private final IngestionTime time;
    private final long started;
    private int requestCount;
    private int retryCount;
    IngestionBudget(IngestionProperties properties, IngestionTime time) {
        this.properties = properties; this.time = time; this.started = time.nanoTime();
    }
    Duration remaining() {
        long remainingNanos = properties.runTimeout().toNanos() - (time.nanoTime() - started);
        if (remainingNanos <= 0) { throw exhausted(); }
        return Duration.ofNanos(remainingNanos);
    }
    Duration beginRequest(int page, int attempt) {
        if (page > properties.maxPages() || requestCount >= properties.maxRequests()) { throw exhausted(); }
        Duration remaining = remaining();
        requestCount++;
        if (attempt > 0) { retryCount++; }
        return remaining;
    }
    boolean waitForRetry(Duration delay) {
        if (requestCount >= properties.maxRequests()) { throw exhausted(); }
        if (delay.compareTo(remaining()) >= 0) { return false; }
        time.sleep(delay);
        return true;
    }
    int requestCount() { return requestCount; }
    int retryCount() { return retryCount; }
    private PublicDataException exhausted() { return new PublicDataException(PublicDataFailure.BUDGET_EXHAUSTED); }
}
