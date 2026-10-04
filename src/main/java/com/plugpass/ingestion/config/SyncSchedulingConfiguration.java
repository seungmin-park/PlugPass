package com.plugpass.ingestion.config;

import com.plugpass.ingestion.scheduler.SyncScheduler;
import com.plugpass.ingestion.service.StationSyncService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(SyncScheduleProperties.class)
public class SyncSchedulingConfiguration {
    @Bean
    @ConditionalOnProperty(name = "plugpass.ingestion.schedule.enabled", havingValue = "true")
    SyncScheduler syncScheduler(StationSyncService stationSyncService, PublicDataProperties publicDataProperties) {
        if (publicDataProperties.serviceKey() == null || publicDataProperties.serviceKey().isBlank()) {
            throw new IllegalArgumentException("scheduled ingestion requires a service key");
        }
        return new SyncScheduler(stationSyncService);
    }
}
