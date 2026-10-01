package com.plugpass.station;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChargerRepository extends JpaRepository<Charger, Long> {
    @Query("select charger from Charger charger where charger.id = :id")
    Optional<Charger> findByIdentity(@Param("id") ChargerId id);
}
