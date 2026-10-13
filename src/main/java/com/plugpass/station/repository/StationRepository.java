package com.plugpass.station.repository;

import com.plugpass.station.domain.Station;

import java.util.Optional;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Long> {
    List<Station> findByProviderInAndStationIdIn(Collection<String> providers, Collection<String> stationIds);
    Optional<Station> findByProviderAndStationId(String provider, String stationId);
}
