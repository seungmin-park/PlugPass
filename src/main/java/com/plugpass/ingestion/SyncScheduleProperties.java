package com.plugpass.ingestion;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("plugpass.ingestion.schedule")
public record SyncScheduleProperties(@DefaultValue("false") boolean enabled, @DefaultValue("PT30M") Duration delay) {
    public SyncScheduleProperties {
        if (delay == null || delay.compareTo(Duration.ofMinutes(30)) < 0) {
            throw new IllegalArgumentException("schedule delay must be at least PT30M");
        }
    }
}
