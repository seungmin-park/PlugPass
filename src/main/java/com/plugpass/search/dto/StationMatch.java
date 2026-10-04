package com.plugpass.search.dto;
import java.util.List;
import com.plugpass.station.domain.GeoPoint;
public record StationMatch(Long id, String provider, String providerStationId, String name, GeoPoint location,
        double distanceMeters, List<ChargerObservation> chargers) {
    public StationMatch { chargers = List.copyOf(chargers); }
}
