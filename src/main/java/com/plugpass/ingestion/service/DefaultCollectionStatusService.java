package com.plugpass.ingestion.service;

import com.plugpass.ingestion.domain.CollectionHealth;
import com.plugpass.ingestion.domain.SyncKind;
import com.plugpass.ingestion.domain.SyncRun;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.dto.CollectionStatus;
import com.plugpass.ingestion.repository.SyncRunRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultCollectionStatusService implements CollectionStatusService {
    private final SyncRunRepository syncRunRepository;
    private final Clock clock;
    public DefaultCollectionStatusService(SyncRunRepository syncRunRepository, Clock clock) {
        this.syncRunRepository = syncRunRepository;
        this.clock = clock;
    }
    @Override
    @Transactional(readOnly = true)
    public CollectionStatus current() {
        Instant catalog = completedAt(SyncKind.CATALOG);
        Instant status = completedAt(SyncKind.STATUS);
        Instant latest = catalog;
        if (status != null && (latest == null || status.isAfter(latest))) { latest = status; }
        SyncRun attempt = syncRunRepository.findFirstByOrderByStartedAtDesc().orElse(null);
        return new CollectionStatus(CollectionHealth.assess(catalog, latest, attempt, clock.instant()).name(),
                catalog, status, attempt == null ? null : attempt.getStartedAt(),
                attempt == null ? null : attempt.getStatus().name(), 300);
    }
    private Instant completedAt(SyncKind kind) {
        return syncRunRepository.findFirstByKindAndStatusOrderByCompletedAtDesc(kind, SyncStatus.SUCCESS)
                .map(SyncRun::getCompletedAt).orElse(null);
    }
}
