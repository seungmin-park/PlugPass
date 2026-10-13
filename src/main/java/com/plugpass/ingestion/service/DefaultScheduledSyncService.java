package com.plugpass.ingestion.service;

import com.plugpass.ingestion.domain.CollectionSchedule;
import com.plugpass.ingestion.domain.SyncKind;
import com.plugpass.ingestion.domain.SyncRun;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.dto.SyncResult;
import com.plugpass.ingestion.repository.SyncRunRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultScheduledSyncService implements ScheduledSyncService {
    private final SyncRunRepository syncRunRepository;
    private final StationSyncService stationSyncService;
    private final StatusSyncService statusSyncService;
    private final Clock clock;
    public DefaultScheduledSyncService(SyncRunRepository syncRunRepository, StationSyncService stationSyncService,
            StatusSyncService statusSyncService, Clock clock) {
        this.syncRunRepository = syncRunRepository;
        this.stationSyncService = stationSyncService;
        this.statusSyncService = statusSyncService;
        this.clock = clock;
    }
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public SyncResult synchronize() {
        Instant catalog = syncRunRepository.findFirstByKindAndStatusOrderByCompletedAtDesc(SyncKind.CATALOG, SyncStatus.SUCCESS)
                .map(SyncRun::getCompletedAt).orElse(null);
        Instant latest = syncRunRepository.findFirstByStatusOrderByCompletedAtDesc(SyncStatus.SUCCESS)
                .map(SyncRun::getCompletedAt).orElse(null);
        if (CollectionSchedule.select(catalog, latest, clock.instant()) == SyncKind.CATALOG) {
            return stationSyncService.synchronize();
        }
        return statusSyncService.synchronize();
    }
}
