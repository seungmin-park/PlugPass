package com.plugpass.ingestion.client;
import java.time.Duration;
public interface IngestionTime {
    long nanoTime();
    void sleep(Duration duration);
}
