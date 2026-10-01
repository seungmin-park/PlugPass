package com.plugpass.ingestion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SyncRunRepository extends JpaRepository<SyncRun, Long> {
    Optional<SyncRun> findFirstByStatusOrderByCompletedAtDesc(SyncStatus status);
}
