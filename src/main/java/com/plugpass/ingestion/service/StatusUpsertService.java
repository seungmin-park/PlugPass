package com.plugpass.ingestion.service;
import com.plugpass.ingestion.dto.ChargerStatusSnapshot;
import java.util.List;
public interface StatusUpsertService { int applyPage(List<ChargerStatusSnapshot> snapshots); }
