package com.plugpass.ingestion.client;

import com.plugpass.ingestion.dto.StationPage;

public interface PublicDataClient extends AutoCloseable {
    StationPage fetchPage(int page);
    @Override
    void close();
}
