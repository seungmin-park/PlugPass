package com.plugpass.ingestion.command;

import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.dto.SyncResult;
import com.plugpass.ingestion.service.StationSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "plugpass.ingestion.command.enabled", havingValue = "true")
public final class IngestionCommand implements ApplicationRunner, ExitCodeGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestionCommand.class);
    private final StationSyncService stationSyncService;
    private int exitCode = 1;

    public IngestionCommand(StationSyncService stationSyncService) { this.stationSyncService = stationSyncService; }

    @Override
    public void run(ApplicationArguments arguments) {
        SyncResult result = stationSyncService.synchronize();
        if (result.status() == SyncStatus.SUCCESS) { exitCode = 0; }
        if (result.status() == SyncStatus.SKIPPED) { exitCode = 2; }
        LOGGER.info("INGESTION_COMMAND status={} processed={} requests={} failure={}",
                result.status(), result.processedCount(), result.requestCount(), result.failureCode());
    }

    @Override public int getExitCode() { return exitCode; }
}
