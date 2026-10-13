package com.plugpass.ingestion.service;

import java.util.function.BiFunction;
import com.plugpass.ingestion.config.IngestionProperties;

import java.time.Duration;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;

final class RetryingPageFetcher<T> {
    private final BiFunction<Integer, Duration, T> request;
    private final IngestionProperties properties;
    RetryingPageFetcher(BiFunction<Integer, Duration, T> request, IngestionProperties properties) {
        this.request = request; this.properties = properties;
    }
    T fetchPage(int page, IngestionBudget budget) {
        for (int attempt = 0; attempt <= properties.maxRetries(); attempt++) {
            Duration remaining = budget.beginRequest(page,attempt);
            try {
                T response = request.apply(page,remaining);
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
