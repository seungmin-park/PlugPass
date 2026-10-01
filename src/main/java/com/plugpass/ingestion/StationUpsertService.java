package com.plugpass.ingestion;

import java.util.List;

public interface StationUpsertService {
    void upsert(StationSnapshot snapshot);
    void upsertPage(List<StationSnapshot> snapshots);
}
