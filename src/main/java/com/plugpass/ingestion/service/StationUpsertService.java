package com.plugpass.ingestion.service;

import com.plugpass.ingestion.dto.StationSnapshot;

import java.util.List;

public interface StationUpsertService {
    void upsert(StationSnapshot snapshot);
    void upsertPage(List<StationSnapshot> snapshots);
}
