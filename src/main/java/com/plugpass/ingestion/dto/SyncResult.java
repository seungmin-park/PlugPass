package com.plugpass.ingestion.dto;

import com.plugpass.ingestion.domain.SyncStatus;
public record SyncResult(Long runId, SyncStatus status, long processedCount, Integer failedPage, String failureCode, long failedPageCount) { }
