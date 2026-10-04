package com.plugpass.recommendation.domain;
import java.util.List;
public record CandidateGroups(List<RankedCandidate> preferred, List<RankedCandidate> requiresConfirmation, List<RankedCandidate> excluded) {
    public CandidateGroups {
        preferred = List.copyOf(preferred);
        requiresConfirmation = List.copyOf(requiresConfirmation);
        excluded = List.copyOf(excluded);
    }
}
