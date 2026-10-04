package com.plugpass.search.dto;
import java.time.Instant;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.domain.ChargerDetails;
import com.plugpass.freshness.domain.FreshnessAssessment;
public record ChargerObservation(String chargerId, ChargerStatus status, String rawStatus, Instant sourceObservedAt,
        Instant collectedAt, FreshnessAssessment freshness, ChargerDetails details) { }
