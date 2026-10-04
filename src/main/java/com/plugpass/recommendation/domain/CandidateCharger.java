package com.plugpass.recommendation.domain;

import java.util.ArrayList;
import java.util.List;
import com.plugpass.freshness.domain.Freshness;
import com.plugpass.freshness.domain.FreshnessAssessment;
import com.plugpass.station.domain.Connector;
import com.plugpass.station.domain.ChargerStatus;

public record CandidateCharger(String connectorCode, ChargerStatus status, FreshnessAssessment freshness,
        String limitYn, String useTime, String delYn) {
    public String exclusionReason(Connector connector) {
        if (!connector.matches(connectorCode)) { return "INCOMPATIBLE_CONNECTOR"; }
        if ("Y".equalsIgnoreCase(delYn)) { return "DELETED_BY_PROVIDER"; }
        if ("Y".equalsIgnoreCase(limitYn)) { return "ACCESS_RESTRICTED"; }
        if (status == ChargerStatus.UNKNOWN) { return "STATUS_UNKNOWN"; }
        if (status != ChargerStatus.AVAILABLE) { return "NOT_AVAILABLE"; }
        if (freshness.freshness() == Freshness.STALE) { return freshness.reasonCode(); }
        return null;
    }
    public List<String> reasonCodes() {
        List<String> reasons = new ArrayList<>();
        if (freshness.freshness() == Freshness.RECENT) { reasons.add("RECENT_AVAILABLE"); }
        if (freshness.freshness() == Freshness.UNVERIFIED) {
            reasons.add("UNVERIFIED_AVAILABLE");
            reasons.add(freshness.reasonCode());
        }
        if (!"N".equalsIgnoreCase(limitYn)) { reasons.add("ACCESS_CONDITIONS_UNVERIFIED"); }
        if (useTime == null || useTime.isBlank()) { reasons.add("OPERATING_HOURS_UNVERIFIED"); }
        if (useTime != null && !useTime.isBlank() && !"24시간".equals(useTime)) { reasons.add("OPERATING_HOURS_REQUIRE_CHECK"); }
        return List.copyOf(reasons);
    }
}
