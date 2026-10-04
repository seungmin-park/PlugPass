package com.plugpass.ingestion;

import java.time.Duration;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;

final class RetryingPageFetcher {
    private final PublicDataClient publicDataClient;
    private final IngestionProperties properties;
    RetryingPageFetcher(PublicDataClient publicDataClient, IngestionProperties properties) {
        this.publicDataClient = publicDataClient; this.properties = properties;
    }
    StationPage fetchPage(int page, IngestionBudget budget) {
        for (int attempt = 0; attempt <= properties.maxRetries(); attempt++) {
            Duration remaining = budget.beginRequest(page,attempt);
            try {
                StationPage response = publicDataClient.fetchPage(page,remaining);
                budget.remaining();
                return response;
            } catch (PublicDataException failure) {
                if (attempt == properties.maxRetries() || !isTemporary(failure.getFailure()) || Thread.currentThread().isInterrupted()) { throw failure; }
                Duration delay = failure.getRetryAfter() == null ? properties.retryDelay() : failure.getRetryAfter();
                if (!budget.waitForRetry(delay)) { throw failure; }
            }
        }
        throw new IllegalStateException("retry loop must return or throw");
    }
    private boolean isTemporary(PublicDataFailure failure) {
        return failure == PublicDataFailure.TIMEOUT || failure == PublicDataFailure.RATE_LIMIT
                || failure == PublicDataFailure.SERVER || failure == PublicDataFailure.TRANSPORT;
    }
}
