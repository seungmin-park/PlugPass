package com.plugpass.search;
import java.util.List;
import com.plugpass.station.GeoPoint;
public record StationDetail(Long id, String provider, String providerStationId, String name, GeoPoint location, List<ChargerObservation> chargers) {
    public StationDetail { chargers = List.copyOf(chargers); }
}
