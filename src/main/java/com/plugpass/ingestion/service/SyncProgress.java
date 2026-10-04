package com.plugpass.ingestion.service;

import com.plugpass.ingestion.dto.StationPage;
import com.plugpass.ingestion.dto.StationSnapshot;

import java.util.HashSet;
import java.util.Set;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;
import com.plugpass.station.domain.ChargerId;

final class SyncProgress {
    private final Set<ChargerId> committedChargerIds = new HashSet<>();
    private int pageNumber = 1;
    private long processedCount;
    private Long totalCount;
    private Integer pageSize;

    int pageNumber() {
        return pageNumber;
    }

    long processedCount() {
        return processedCount;
    }

    void validatePage(StationPage page) {
        if (page.pageNumber() != pageNumber || (totalCount != null && page.totalCount() != totalCount)
                || (pageSize != null && page.pageSize() != pageSize)) {
            throw new PublicDataException(PublicDataFailure.CONTRACT);
        }
        Set<ChargerId> pageChargerIds = new HashSet<>();
        for (StationSnapshot snapshot : page.snapshots()) {
            ChargerId chargerId = snapshot.chargerId();
            if (committedChargerIds.contains(chargerId) || !pageChargerIds.add(chargerId)) {
                throw new PublicDataException(PublicDataFailure.CONTRACT);
            }
        }
    }

    void recordCommittedPage(StationPage page) {
        totalCount = page.totalCount();
        pageSize = page.pageSize();
        page.snapshots().forEach(snapshot -> committedChargerIds.add(snapshot.chargerId()));
        processedCount += page.snapshots().size();
    }

    void advancePage() {
        pageNumber = Math.incrementExact(pageNumber);
    }

    void requireComplete() {
        if (totalCount == null || processedCount != totalCount) {
            throw new PublicDataException(PublicDataFailure.CONTRACT);
        }
    }
}
