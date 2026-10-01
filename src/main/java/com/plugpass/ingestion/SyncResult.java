package com.plugpass.ingestion;
public record SyncResult(Long runId, SyncStatus status, long processedCount, Integer failedPage, String failureCode, long failedPageCount) { }
