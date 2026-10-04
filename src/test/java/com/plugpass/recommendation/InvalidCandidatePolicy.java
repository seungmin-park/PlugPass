package com.plugpass.recommendation;

import com.plugpass.station.StationRepository;

// Checker input only: no Spring/JPA annotation, so this class cannot join the app.
public final class InvalidCandidatePolicy {
    private StationRepository forbiddenRepository;
}
