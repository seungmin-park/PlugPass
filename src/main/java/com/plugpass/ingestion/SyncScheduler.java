package com.plugpass.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class SyncScheduler {
    private static final Logger logger = LoggerFactory.getLogger(SyncScheduler.class);
    private final StationSyncService stationSyncService;
    public SyncScheduler(StationSyncService stationSyncService) { this.stationSyncService = stationSyncService; }
    @Scheduled(fixedDelayString = "${plugpass.ingestion.schedule.delay:PT30M}", initialDelayString = "${plugpass.ingestion.schedule.delay:PT30M}")
    public void tick() {
        try { stationSyncService.synchronize(); }
        catch (RuntimeException failure) { logger.warn("Scheduled ingestion ended with INTERNAL failure; next tick remains enabled"); }
    }
}
