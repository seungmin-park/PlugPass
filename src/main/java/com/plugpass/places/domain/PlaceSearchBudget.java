package com.plugpass.places.domain;

import java.time.Duration;
import java.time.Instant;

/** Caps outbound place searches per minute so anonymous callers cannot drain the operator's Kakao quota. */
public final class PlaceSearchBudget {
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private final int searchesPerMinute;
    private Instant windowStart;
    private int used;

    public PlaceSearchBudget(int searchesPerMinute) {
        if (searchesPerMinute < 1) { throw new IllegalArgumentException("searches per minute must be positive"); }
        this.searchesPerMinute = searchesPerMinute;
    }

    public synchronized boolean tryAcquire(Instant now) {
        if (windowStart == null || !now.isBefore(windowStart.plus(WINDOW))) {
            windowStart = now;
            used = 0;
        }
        if (used >= searchesPerMinute) { return false; }
        used++;
        return true;
    }
}
