package com.plugpass.ingestion;
import java.time.Duration;

public interface PublicDataClient extends AutoCloseable {
    StationPage fetchPage(int page);
    StationPage fetchPage(int page, Duration remaining);
    @Override
    void close();
}
