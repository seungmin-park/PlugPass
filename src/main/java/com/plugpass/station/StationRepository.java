package com.plugpass.station;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByProviderAndStationId(String provider, String stationId);
}
