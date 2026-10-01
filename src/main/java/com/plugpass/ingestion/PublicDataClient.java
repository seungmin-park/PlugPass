package com.plugpass.ingestion;

public interface PublicDataClient extends AutoCloseable {
    StationPage fetchPage(int page);
    @Override
    void close();
}
