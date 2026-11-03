package com.plugpass.station.repository;

/** Lightweight bounding-box candidate: enough to rank by distance before loading chargers. */
public interface CandidateStationLocation {
    Long getStationId();
    double getLatitude();
    double getLongitude();
}
