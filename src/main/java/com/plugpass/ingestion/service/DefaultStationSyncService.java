package com.plugpass.ingestion.service;

import com.plugpass.ingestion.config.IngestionProperties;
import com.plugpass.ingestion.client.IngestionTime;

import com.plugpass.ingestion.client.PublicDataClient;
import com.plugpass.ingestion.domain.SyncRun;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.dto.StationPage;
import com.plugpass.ingestion.dto.SyncResult;
import com.plugpass.ingestion.repository.SyncRunRepository;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;
import com.plugpass.exception.PublicDataException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultStationSyncService implements StationSyncService {
    private final PublicDataClient publicDataClient;
    private final StationUpsertService stationUpsertService;
    private final SyncRunRepository syncRunRepository;
    private final Clock clock;
    private final IngestionProperties ingestionProperties;
    private final IngestionTime ingestionTime;
    private final AtomicBoolean running = new AtomicBoolean();

    public DefaultStationSyncService(PublicDataClient publicDataClient, StationUpsertService stationUpsertService,
            SyncRunRepository syncRunRepository, Clock clock, IngestionProperties ingestionProperties, IngestionTime ingestionTime) {
        this.publicDataClient = publicDataClient;
        this.stationUpsertService = stationUpsertService;
        this.syncRunRepository = syncRunRepository;
        this.clock = clock;
        this.ingestionProperties = ingestionProperties;
        this.ingestionTime = ingestionTime;
    }
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public SyncResult synchronize() {
        if (!running.compareAndSet(false,true)) { return new SyncResult(null,SyncStatus.SKIPPED,0,null,null,0,0,0); }
        try { return runSynchronization(); }
        finally { running.set(false); }
    }
    private SyncResult runSynchronization() {
        SyncRun run = syncRunRepository.save(SyncRun.builder().startedAt(clock.instant()).build());
        SyncProgress progress = new SyncProgress();
        IngestionBudget budget = new IngestionBudget(ingestionProperties,ingestionTime);
        RetryingPageFetcher fetcher = new RetryingPageFetcher(publicDataClient,ingestionProperties);
        try {
            while (true) {
                StationPage page = fetcher.fetchPage(progress.pageNumber(),budget);
                progress.validatePage(page);
                stationUpsertService.upsertPage(page.snapshots());
                progress.recordCommittedPage(page);
                if (!page.hasNext()) { break; }
                progress.advancePage();
            }
            progress.requireComplete();
            return complete(run, budget, progress.processedCount(), null, null);
        } catch (PublicDataException failure) {
            return complete(run, budget, progress.processedCount(), progress.pageNumber(), failure.getFailure().name());
        } catch (DataAccessException failure) {
            return complete(run, budget, progress.processedCount(), progress.pageNumber(), "STORAGE");
        } catch (RuntimeException failure) {
            complete(run, budget, progress.processedCount(), progress.pageNumber(), "INTERNAL");
            throw failure;
        }
    }
    private SyncResult complete(SyncRun run, IngestionBudget budget, long processedCount, Integer failedPage, String failureCode) {
        run.complete(processedCount, failedPage, failureCode, clock.instant(),budget.requestCount(),budget.retryCount());
        return syncRunRepository.save(run).result();
    }
}
