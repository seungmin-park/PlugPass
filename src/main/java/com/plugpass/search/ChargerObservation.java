package com.plugpass.search;
import java.time.Instant;
import com.plugpass.station.ChargerStatus;
import com.plugpass.station.ChargerDetails;
import com.plugpass.freshness.FreshnessAssessment;
public record ChargerObservation(String chargerId, ChargerStatus status, String rawStatus, Instant sourceObservedAt,
        Instant collectedAt, FreshnessAssessment freshness, ChargerDetails details) { }
