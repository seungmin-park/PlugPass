package com.plugpass.station.repository;

import com.plugpass.station.domain.Charger;
import com.plugpass.station.domain.ChargerId;

import java.util.Optional;
import java.util.Set;
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

    @Query("""
            select charger from Charger charger join fetch charger.station station
            where station.location.latitude between :minLatitude and :maxLatitude
              and ((:minLongitude <= :maxLongitude and station.location.longitude between :minLongitude and :maxLongitude)
                or (:minLongitude > :maxLongitude and (station.location.longitude >= :minLongitude or station.location.longitude <= :maxLongitude)))
              and charger.details.connectorCode in :connectorCodes
            """)
    List<Charger> findSearchCandidates(@Param("minLatitude") double minLatitude, @Param("maxLatitude") double maxLatitude,
            @Param("minLongitude") double minLongitude, @Param("maxLongitude") double maxLongitude,
            @Param("connectorCodes") Set<String> connectorCodes);

    @Query("select charger from Charger charger where charger.id = :id")
    Optional<Charger> findByIdentity(@Param("id") ChargerId id);
}
