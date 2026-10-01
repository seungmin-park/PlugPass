package com.plugpass.search;

import jakarta.validation.Valid;
import com.plugpass.search.request.StationSearchRequest;
import com.plugpass.search.response.StationSearchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stations")
public class StationController {
    private final StationQueryService stationQueryService;
    public StationController(StationQueryService stationQueryService) { this.stationQueryService = stationQueryService; }
    @GetMapping
    public StationSearchResponse search(@Valid @ModelAttribute StationSearchRequest request) {
        return StationSearchResponse.from(stationQueryService.search(request.toQuery()));
    }
}
