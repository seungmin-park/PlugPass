package com.plugpass.ingestion.dto.response;
import java.util.List;
public record PublicStatusResponse(int pageNumber, int reportedRowCount, long totalCount, List<PublicStatusItem> items) { }
