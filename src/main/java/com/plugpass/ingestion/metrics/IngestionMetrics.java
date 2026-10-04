package com.plugpass.ingestion.metrics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import com.plugpass.freshness.domain.Freshness;
import com.plugpass.freshness.domain.FreshnessPolicy;
import com.plugpass.ingestion.domain.SyncRun;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.dto.SyncResult;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.station.repository.ChargerRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class IngestionMetrics {
    private static final Logger logger = LoggerFactory.getLogger(IngestionMetrics.class);
    private static final Set<String> failureCodes = Set.of("AUTHENTICATION","RATE_LIMIT","SERVER","CONTRACT",
            "TIMEOUT","TRANSPORT","BUDGET_EXHAUSTED","STORAGE","INTERNAL");
    private final MeterRegistry meterRegistry;
    private final SyncRunRepository syncRunRepository;
    private final ChargerRepository chargerRepository;
    private final FreshnessPolicy freshnessPolicy;
    private final Clock clock;

    public IngestionMetrics(MeterRegistry meterRegistry, SyncRunRepository syncRunRepository,
            ChargerRepository chargerRepository, FreshnessPolicy freshnessPolicy, Clock clock) {
        this.meterRegistry = meterRegistry;
        this.syncRunRepository = syncRunRepository;
        this.chargerRepository = chargerRepository;
        this.freshnessPolicy = freshnessPolicy;
        this.clock = clock;
        Gauge.builder("plugpass.ingestion.has.success",this,metrics -> metrics.lastSuccess() == null ? 0 : 1)
                .description("Whether a complete successful ingestion exists").register(meterRegistry);
        Gauge.builder("plugpass.ingestion.last.success.age",this,IngestionMetrics::lastSuccessAge)
                .baseUnit("seconds").description("Elapsed since complete success; NaN before first success").register(meterRegistry);
        for (Freshness freshness : Freshness.values()) {
            Gauge.builder("plugpass.data.chargers",this,metrics -> metrics.countFreshness(freshness))
                    .tag("freshness",freshness.name()).description("Stored charger observations by current freshness").register(meterRegistry);
        }
    }
    public void recordCompletion(SyncResult result) {
        String failure = failureCode(result.failureCode());
        meterRegistry.counter("plugpass.ingestion.runs","outcome",result.status().name()).increment();
        meterRegistry.counter("plugpass.ingestion.records").increment(result.processedCount());
        meterRegistry.counter("plugpass.ingestion.requests").increment(result.requestCount());
        meterRegistry.counter("plugpass.ingestion.retries").increment(result.retryCount());
        if (!"NONE".equals(failure)) { meterRegistry.counter("plugpass.ingestion.failures","cause",failure).increment(); }
        logger.info("ingestion runId={} outcome={} processed={} failedPage={} failure={} requests={} retries={}",
                result.runId(),result.status(),result.processedCount(),result.failedPage(),failure,result.requestCount(),result.retryCount());
    }
    private String failureCode(String code) {
        if (code == null) { return "NONE"; }
        if (failureCodes.contains(code)) { return code; }
        return "INTERNAL";
    }
    private Instant lastSuccess() {
        return syncRunRepository.findFirstByStatusOrderByCompletedAtDesc(SyncStatus.SUCCESS).map(SyncRun::getCompletedAt).orElse(null);
    }
    private double lastSuccessAge() {
        Instant success = lastSuccess();
        if (success == null) { return Double.NaN; }
        return Math.max(0,Duration.between(success,clock.instant()).toMillis()/1000.0);
    }
    private long countFreshness(Freshness freshness) {
        Instant now = clock.instant();
        Instant oldestRecent = freshnessPolicy.oldestRecentObservationAt(now);
        if (freshness == Freshness.RECENT) { return chargerRepository.countBySourceObservedAtBetween(oldestRecent,now); }
        if (freshness == Freshness.STALE) { return chargerRepository.countBySourceObservedAtBefore(oldestRecent); }
        return chargerRepository.countBySourceObservedAtIsNull()+chargerRepository.countBySourceObservedAtAfter(now);
    }
}
