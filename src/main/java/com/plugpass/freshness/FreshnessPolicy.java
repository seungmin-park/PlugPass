package com.plugpass.freshness;

import java.time.Duration;
import java.time.Instant;

public final class FreshnessPolicy {
    private final Duration maxAge;
    public FreshnessPolicy(Duration maxAge) {
        if (maxAge == null || maxAge.isNegative() || maxAge.isZero()) {
            throw new IllegalArgumentException("maxAge must be positive");
        }
        this.maxAge = maxAge;
    }
    public Freshness evaluate(Instant sourceObservedAt, Instant now) {
        return assess(sourceObservedAt, now).freshness();
    }
    public FreshnessAssessment assess(Instant sourceObservedAt, Instant now) {
        if (now == null) { throw new IllegalArgumentException("now must not be null"); }
        if (sourceObservedAt == null) {
            return new FreshnessAssessment(Freshness.UNVERIFIED,"SOURCE_OBSERVED_AT_MISSING");
        }
        if (sourceObservedAt.isAfter(now)) {
            return new FreshnessAssessment(Freshness.UNVERIFIED,"SOURCE_OBSERVED_AT_IN_FUTURE");
        }
        if (Duration.between(sourceObservedAt, now).compareTo(maxAge) > 0) {
            return new FreshnessAssessment(Freshness.STALE,"MAX_AGE_EXCEEDED");
        }
        return new FreshnessAssessment(Freshness.RECENT,"WITHIN_MAX_AGE");
    }
}
