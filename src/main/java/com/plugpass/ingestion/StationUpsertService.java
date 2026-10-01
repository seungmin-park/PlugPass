package com.plugpass.ingestion;

public interface StationUpsertService {
    void upsert(StationSnapshot snapshot);
}
