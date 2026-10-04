package com.plugpass.ingestion.repository;

import com.plugpass.ingestion.domain.SyncRun;
import com.plugpass.ingestion.domain.SyncStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SyncRunRepository extends JpaRepository<SyncRun, Long> {
    Optional<SyncRun> findFirstByStatusOrderByCompletedAtDesc(SyncStatus status);
}
