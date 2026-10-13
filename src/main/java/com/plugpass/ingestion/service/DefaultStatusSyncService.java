package com.plugpass.ingestion.service;

import com.plugpass.ingestion.client.IngestionLock;
import com.plugpass.ingestion.client.IngestionTime;
import com.plugpass.ingestion.client.PublicStatusClient;
import com.plugpass.ingestion.config.IngestionProperties;
import com.plugpass.ingestion.domain.SyncRun;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.dto.StatusPage;
import com.plugpass.ingestion.dto.SyncResult;
import com.plugpass.ingestion.dto.ChargerStatusSnapshot;
import com.plugpass.ingestion.metrics.IngestionMetrics;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;
import com.plugpass.station.domain.ChargerId;
import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class DefaultStatusSyncService implements StatusSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStatusSyncService.class);
    private static final IngestionProperties BUDGET = new IngestionProperties(20,40,1,Duration.ofSeconds(120),Duration.ofSeconds(1));
    private final PublicStatusClient publicStatusClient;
    private final StatusUpsertService statusUpsertService;
    private final SyncRunRepository syncRunRepository;
    private final IngestionLock ingestionLock;
    private final IngestionTime ingestionTime;
    private final IngestionMetrics ingestionMetrics;
    private final Clock clock;

    public DefaultStatusSyncService(PublicStatusClient publicStatusClient, StatusUpsertService statusUpsertService,
            SyncRunRepository syncRunRepository, IngestionLock ingestionLock, IngestionTime ingestionTime,
            IngestionMetrics ingestionMetrics, Clock clock) {
        this.publicStatusClient=publicStatusClient; this.statusUpsertService=statusUpsertService;
        this.syncRunRepository=syncRunRepository; this.ingestionLock=ingestionLock; this.ingestionTime=ingestionTime;
        this.ingestionMetrics=ingestionMetrics; this.clock=clock;
    }
    @Override @Transactional(propagation = Propagation.NEVER)
    public SyncResult synchronize() {
        Optional<IngestionLock.Lease> acquired = ingestionLock.acquire();
        if (acquired.isEmpty()) { return new SyncResult(null,SyncStatus.SKIPPED,0,null,null,0,0,0); }
        try (IngestionLock.Lease lease = acquired.orElseThrow()) { return collect(lease); }
    }
    private SyncResult collect(IngestionLock.Lease lease) {
        SyncRun run = syncRunRepository.save(SyncRun.statusRun(clock.instant()));
        IngestionBudget budget = new IngestionBudget(BUDGET,ingestionTime);
        RetryingPageFetcher<StatusPage> fetcher = new RetryingPageFetcher<>(publicStatusClient::fetchStatusPage,BUDGET);
        long processed = 0;
        int pageNumber = 1;
        try {
            while (true) {
                lease.requireOwnership();
                StatusPage page = fetcher.fetchPage(pageNumber,budget);
                requireValidPage(page,pageNumber);
                lease.requireOwnership();
                int matched = statusUpsertService.applyPage(page.snapshots());
                processed += page.snapshots().size();
                LOGGER.info("status page={} processed={} missingCatalog={}",pageNumber,page.snapshots().size(),page.snapshots().size()-matched);
                if (!page.hasNext()) { break; }
                pageNumber++;
            }
            return finish(run,budget,processed,null,null);
        } catch (PublicDataException failure) {
            return finish(run,budget,processed,pageNumber,failure.getFailure().name());
        } catch (DataAccessException failure) {
            return finish(run,budget,processed,pageNumber,"STORAGE");
        } catch (RuntimeException failure) {
            finish(run,budget,processed,pageNumber,"INTERNAL");
            throw failure;
        }
    }
    private void requireValidPage(StatusPage page, int expectedPage) {
        if (page.pageNumber()!=expectedPage || page.pageSize()<1 || page.totalCount()<0
                || page.snapshots().size()>page.pageSize() || (page.snapshots().isEmpty() && page.hasNext())) {
            throw new PublicDataException(PublicDataFailure.CONTRACT);
        }
        Set<ChargerId> identities = new HashSet<>();
        for (ChargerStatusSnapshot snapshot : page.snapshots()) {
            if (!identities.add(snapshot.chargerId())) { throw new PublicDataException(PublicDataFailure.CONTRACT); }
        }
        // The provider's rolling ten-minute window may shift between pages. Overlap and timestamp ordering absorb repeats.
    }
    private SyncResult finish(SyncRun run, IngestionBudget budget, long processed, Integer page, String failure) {
        run.complete(processed,page,failure,clock.instant(),budget.requestCount(),budget.retryCount());
        SyncResult result = syncRunRepository.save(run).result();
        ingestionMetrics.recordCompletion(result);
        return result;
    }
}
