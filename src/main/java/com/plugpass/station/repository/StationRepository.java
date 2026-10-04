package com.plugpass.station.repository;

import com.plugpass.station.domain.Station;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByProviderAndStationId(String provider, String stationId);
}
