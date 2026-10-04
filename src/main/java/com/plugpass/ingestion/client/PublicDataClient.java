package com.plugpass.ingestion.client;
import java.time.Duration;
import com.plugpass.ingestion.dto.StationPage;
public interface PublicDataClient extends AutoCloseable {
    StationPage fetchPage(int page);
    StationPage fetchPage(int page, Duration remaining);
    @Override void close();
}
