package com.plugpass.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PersistenceProbeRepository extends JpaRepository<PersistenceProbe, UUID> {
}
