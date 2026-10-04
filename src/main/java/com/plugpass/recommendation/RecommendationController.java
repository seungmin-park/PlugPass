package com.plugpass.recommendation;
import jakarta.validation.Valid;
import com.plugpass.recommendation.request.RecommendationRequest;
import com.plugpass.recommendation.response.RecommendationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class RecommendationController {
    private final RecommendationService recommendationService;
    public RecommendationController(RecommendationService recommendationService) { this.recommendationService = recommendationService; }
    @GetMapping("/api/v1/recommendations")
    public RecommendationResponse recommend(@Valid @ModelAttribute RecommendationRequest request) {
        return RecommendationResponse.from(recommendationService.recommend(request.toQuery(),request.excludeStationId()));
    }
}
