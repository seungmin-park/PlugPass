package com.plugpass.recommendation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.plugpass.freshness.Freshness;

public final class CandidatePolicy {
    public CandidateGroups rank(List<StationCandidate> candidates) {
        List<RankedCandidate> preferred = new ArrayList<>();
        List<RankedCandidate> confirmation = new ArrayList<>();
        List<RankedCandidate> excluded = new ArrayList<>();
        List<StationCandidate> ordered = candidates.stream().sorted(Comparator.comparingDouble(StationCandidate::distanceMeters)
                .thenComparing(StationCandidate::id)).toList();
        for (StationCandidate candidate : ordered) {
            List<CandidateCharger> eligible = candidate.chargers().stream()
                    .filter(charger -> charger.exclusionReason(candidate.connector()) == null)
                    .sorted(Comparator.comparing(charger -> charger.freshness().freshness())).toList();
            if (eligible.isEmpty()) {
                List<String> reasons = candidate.chargers().stream().map(charger -> charger.exclusionReason(candidate.connector())).distinct().toList();
                excluded.add(ranked(candidate,reasons.isEmpty() ? List.of("NO_COMPATIBLE_CHARGER") : reasons));
                continue;
            }
            Freshness best = eligible.getFirst().freshness().freshness();
            List<String> reasons = eligible.stream().filter(charger -> charger.freshness().freshness() == best)
                    .flatMap(charger -> charger.reasonCodes().stream()).distinct().toList();
            RankedCandidate ranked = ranked(candidate,reasons);
            if (best == Freshness.RECENT) { preferred.add(ranked); }
            if (best == Freshness.UNVERIFIED) { confirmation.add(ranked); }
        }
        return new CandidateGroups(preferred,confirmation,excluded);
    }
    private RankedCandidate ranked(StationCandidate candidate, List<String> reasons) {
        return new RankedCandidate(candidate.id(),candidate.name(),candidate.distanceMeters(),reasons);
    }
}
