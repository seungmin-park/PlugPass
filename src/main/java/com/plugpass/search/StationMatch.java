package com.plugpass.search;
import java.util.List;
import com.plugpass.station.GeoPoint;
public record StationMatch(Long id, String provider, String providerStationId, String name, GeoPoint location,
        double distanceMeters, List<ChargerObservation> chargers) {
    public StationMatch { chargers = List.copyOf(chargers); }
}
