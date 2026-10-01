package com.plugpass.ingestion;

import java.time.Clock;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;
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

    public DefaultStationSyncService(PublicDataClient publicDataClient, StationUpsertService stationUpsertService,
            SyncRunRepository syncRunRepository, Clock clock) {
        this.publicDataClient = publicDataClient;
        this.stationUpsertService = stationUpsertService;
        this.syncRunRepository = syncRunRepository;
        this.clock = clock;
    }
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public synchronized SyncResult synchronize() {
        SyncRun run = syncRunRepository.save(SyncRun.builder().startedAt(clock.instant()).build());
        long processedCount = 0;
        int pageNumber = 1;
        Long totalCount = null;
        Integer pageSize = null;
        try {
            while (true) {
                StationPage page = publicDataClient.fetchPage(pageNumber);
                if (page.pageNumber() != pageNumber || (totalCount != null && page.totalCount() != totalCount)
                        || (pageSize != null && page.pageSize() != pageSize)) {
                    throw new PublicDataException(PublicDataFailure.CONTRACT);
                }
                totalCount = page.totalCount();
                pageSize = page.pageSize();
                stationUpsertService.upsertPage(page.snapshots());
                processedCount += page.snapshots().size();
                if (!page.hasNext()) { break; }
                pageNumber = Math.incrementExact(pageNumber);
            }
            return complete(run, processedCount, null, null);
        } catch (PublicDataException failure) {
            return complete(run, processedCount, pageNumber, failure.getFailure().name());
        } catch (DataAccessException failure) {
            return complete(run, processedCount, pageNumber, "STORAGE");
        }
    }
    private SyncResult complete(SyncRun run, long processedCount, Integer failedPage, String failureCode) {
        run.complete(processedCount, failedPage, failureCode, clock.instant());
        return syncRunRepository.save(run).result();
    }
}
