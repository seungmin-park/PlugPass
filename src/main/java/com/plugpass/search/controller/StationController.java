package com.plugpass.search.controller;

import com.plugpass.search.service.StationQueryService;

import jakarta.validation.Valid;
import com.plugpass.search.dto.request.StationSearchRequest;
import com.plugpass.search.dto.response.StationSearchResponse;
import com.plugpass.search.dto.response.StationDetailResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stations")
public class StationController {
    private final StationQueryService stationQueryService;
    public StationController(StationQueryService stationQueryService) { this.stationQueryService = stationQueryService; }
    @GetMapping("/{stationId}")
    public StationDetailResponse detail(@PathVariable Long stationId) {
        return StationDetailResponse.from(stationQueryService.detail(stationId));
    }

    @GetMapping
    public StationSearchResponse search(@Valid @ModelAttribute StationSearchRequest request) {
        return StationSearchResponse.from(stationQueryService.search(request.toQuery()));
    }
}
