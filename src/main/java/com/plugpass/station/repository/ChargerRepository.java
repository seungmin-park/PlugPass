package com.plugpass.station.repository;

import com.plugpass.station.domain.Charger;
import com.plugpass.station.domain.ChargerId;

import java.util.Optional;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChargerRepository extends JpaRepository<Charger, Long> {
    long countBySourceObservedAtBetween(Instant oldestRecent, Instant now);
    long countBySourceObservedAtBefore(Instant oldestRecent);
    long countBySourceObservedAtIsNull();
    long countBySourceObservedAtAfter(Instant now);

    List<Charger> findByStationDatabaseId(Long stationId);

    @Query("select charger from Charger charger join fetch charger.station")
    List<Charger> findAllWithStation();

    @Query("select charger from Charger charger where charger.id = :id")
    Optional<Charger> findByIdentity(@Param("id") ChargerId id);
}
