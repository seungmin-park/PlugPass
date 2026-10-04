package com.plugpass.recommendation.domain;

import com.plugpass.station.repository.StationRepository;

// Checker input only: no Spring/JPA annotation, so this class cannot join the app.
public final class InvalidCandidatePolicy {
    private StationRepository forbiddenRepository;
}
