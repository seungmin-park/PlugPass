package com.plugpass.ingestion.client;
import java.time.Duration;
import com.plugpass.ingestion.dto.StatusPage;
public interface PublicStatusClient { StatusPage fetchStatusPage(int page, Duration remaining); }
