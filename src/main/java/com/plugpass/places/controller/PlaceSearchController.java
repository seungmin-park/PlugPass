package com.plugpass.places.controller;

import com.plugpass.places.dto.request.PlaceSearchRequest;
import com.plugpass.places.dto.response.PlaceSearchResponse;
import com.plugpass.places.service.PlaceSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class PlaceSearchController {
    private final PlaceSearchService placeSearchService;
    public PlaceSearchController(PlaceSearchService placeSearchService) { this.placeSearchService = placeSearchService; }
    @GetMapping("/api/v1/places")
    public PlaceSearchResponse search(@Valid @ModelAttribute PlaceSearchRequest request) {
        return new PlaceSearchResponse(placeSearchService.search(request.query()));
    }
}
