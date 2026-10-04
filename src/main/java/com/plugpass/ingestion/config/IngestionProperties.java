package com.plugpass.ingestion.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("plugpass.ingestion")
public record IngestionProperties(@DefaultValue("10") int maxPages, @DefaultValue("20") int maxRequests,
        @DefaultValue("1") int maxRetries, @DefaultValue("PT120S") Duration runTimeout, @DefaultValue("PT1S") Duration retryDelay) {
    public IngestionProperties {
        if (maxPages < 1 || maxPages > 10) { throw new IllegalArgumentException("maxPages must be between 1 and 10"); }
        if (maxRequests < 1 || maxRequests > 20) { throw new IllegalArgumentException("maxRequests must be between 1 and 20"); }
        if (maxRetries < 0 || maxRetries > 1) { throw new IllegalArgumentException("maxRetries must be between 0 and 1"); }
        if (runTimeout == null || runTimeout.isZero() || runTimeout.isNegative() || runTimeout.compareTo(Duration.ofSeconds(120)) > 0) {
            throw new IllegalArgumentException("runTimeout must be positive and at most PT120S");
        }
        if (retryDelay == null || retryDelay.isNegative() || retryDelay.compareTo(runTimeout) > 0) {
            throw new IllegalArgumentException("retryDelay must be nonnegative and at most runTimeout");
        }
    }
}
