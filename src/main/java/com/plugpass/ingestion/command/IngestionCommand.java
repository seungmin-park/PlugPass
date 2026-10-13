package com.plugpass.ingestion.command;

import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.dto.SyncResult;
import com.plugpass.ingestion.service.StationSyncService;
import com.plugpass.ingestion.service.StatusSyncService;
import com.plugpass.ingestion.service.ScheduledSyncService;
import org.springframework.beans.factory.annotation.Value;
import java.util.Set;
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
    private final StatusSyncService statusSyncService;
    private final ScheduledSyncService scheduledSyncService;
    private final String mode;
    private int exitCode = 1;

    public IngestionCommand(StationSyncService stationSyncService, StatusSyncService statusSyncService,
            ScheduledSyncService scheduledSyncService, @Value("${plugpass.ingestion.command.mode:catalog}") String mode) {
        if (!Set.of("catalog", "status", "auto").contains(mode)) {
            throw new IllegalArgumentException("ingestion command mode must be catalog, status or auto");
        }
        this.stationSyncService = stationSyncService;
        this.statusSyncService = statusSyncService;
        this.scheduledSyncService = scheduledSyncService;
        this.mode = mode;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        SyncResult result = switch (mode) {
            case "auto" -> scheduledSyncService.synchronize();
            case "status" -> statusSyncService.synchronize();
            default -> stationSyncService.synchronize();
        };
        if (result.status() == SyncStatus.SUCCESS) { exitCode = 0; }
        if (result.status() == SyncStatus.SKIPPED) { exitCode = 2; }
        LOGGER.info("INGESTION_COMMAND status={} processed={} requests={} failure={}",
                result.status(), result.processedCount(), result.requestCount(), result.failureCode());
    }

    @Override public int getExitCode() { return exitCode; }
}
