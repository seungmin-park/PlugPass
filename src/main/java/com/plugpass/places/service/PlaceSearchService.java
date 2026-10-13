package com.plugpass.places.service;

import com.plugpass.places.dto.PlaceMatch;
import java.util.List;
public interface PlaceSearchService { List<PlaceMatch> search(String query); }
