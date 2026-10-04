package com.plugpass.recommendation.response;
import java.util.List;
import com.plugpass.recommendation.CandidateGroups;
import com.plugpass.recommendation.RankedCandidate;
public record RecommendationResponse(List<CandidateResponse> preferred, List<CandidateResponse> requiresConfirmation, List<CandidateResponse> excluded) {
    public static RecommendationResponse from(CandidateGroups groups) {
        return new RecommendationResponse(groups.preferred().stream().map(CandidateResponse::from).toList(),
                groups.requiresConfirmation().stream().map(CandidateResponse::from).toList(),groups.excluded().stream().map(CandidateResponse::from).toList());
    }
    public record CandidateResponse(Long id, String name, double distanceMeters, List<String> reasonCodes) {
        static CandidateResponse from(RankedCandidate candidate) {
            return new CandidateResponse(candidate.id(),candidate.name(),candidate.distanceMeters(),candidate.reasonCodes());
        }
    }
}
