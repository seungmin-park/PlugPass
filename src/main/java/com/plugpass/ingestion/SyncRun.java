package com.plugpass.ingestion;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, updatable = false) private Instant startedAt;
    private Instant completedAt;
    @Column(nullable = false) @Enumerated(EnumType.STRING) private SyncStatus status;
    private long processedCount;
    private Integer failedPage;
    @Column(nullable = false) private long failedPageCount;
    private String failureCode;

    @Builder
    private SyncRun(Instant startedAt) {
        if (startedAt == null) { throw new IllegalArgumentException("startedAt must not be null"); }
        this.startedAt = startedAt;
        this.status = SyncStatus.RUNNING;
    }
    public void complete(long processedCount, Integer failedPage, String failureCode, Instant completedAt) {
        if (status != SyncStatus.RUNNING) { throw new IllegalStateException("sync run is already completed"); }
        if (processedCount < 0 || completedAt == null || completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("invalid sync completion");
        }
        if ((failedPage == null) != (failureCode == null) || (failedPage != null && (failedPage < 1 || failureCode.isBlank()))) {
            throw new IllegalArgumentException("failure page and code must be supplied together");
        }
        this.processedCount = processedCount;
        this.failedPage = failedPage;
        this.failedPageCount = failedPage == null ? 0 : 1;
        this.failureCode = failureCode;
        this.completedAt = completedAt;
        this.status = completionStatus(processedCount, failedPage);
    }
    private SyncStatus completionStatus(long processedCount, Integer failedPage) {
        if (failedPage == null) { return SyncStatus.SUCCESS; }
        if (processedCount > 0) { return SyncStatus.PARTIAL_FAILURE; }
        return SyncStatus.FAILURE;
    }
    public SyncResult result() { return new SyncResult(id, status, processedCount, failedPage, failureCode, failedPageCount); }
}
