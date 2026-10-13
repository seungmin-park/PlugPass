package com.plugpass.places.client;

import com.plugpass.places.dto.PlaceMatch;
import java.util.List;
public interface PlaceSearchClient { List<PlaceMatch> search(String query); }
