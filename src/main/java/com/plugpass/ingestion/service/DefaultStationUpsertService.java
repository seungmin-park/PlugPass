package com.plugpass.ingestion.service;

import com.plugpass.ingestion.dto.StationSnapshot;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.plugpass.station.domain.Charger;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.domain.Station;
import com.plugpass.station.repository.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultStationUpsertService implements StationUpsertService {
    private final StationRepository stationRepository;
    private final ChargerRepository chargerRepository;

    public DefaultStationUpsertService(StationRepository stationRepository, ChargerRepository chargerRepository) {
        this.stationRepository = stationRepository;
        this.chargerRepository = chargerRepository;
    }

    @Override
    @Transactional
    public void upsertPage(List<StationSnapshot> snapshots) {
        if (snapshots.isEmpty()) { return; }
        Map<ChargerId, Charger> chargers = chargerRepository.findByIdentities(snapshots.stream().map(StationSnapshot::chargerId).toList())
                .stream().collect(Collectors.toMap(Charger::getId, charger -> charger));
        Map<StationIdentity, Station> stations = stationRepository.findByProviderInAndStationIdIn(
                snapshots.stream().map(snapshot -> snapshot.chargerId().provider()).distinct().toList(),
                snapshots.stream().map(snapshot -> snapshot.chargerId().stationId()).distinct().toList())
                .stream().collect(Collectors.toMap(station -> new StationIdentity(station.getProvider(),station.getStationId()),station -> station));
        for (StationSnapshot snapshot : snapshots) {
            Charger existing = chargers.get(snapshot.chargerId());
            if (existing != null && !existing.acceptsObservation(snapshot.sourceObservedAt())) { continue; }
            StationIdentity identity = new StationIdentity(snapshot.chargerId().provider(),snapshot.chargerId().stationId());
            Station station = stations.computeIfAbsent(identity, ignored -> stationRepository.save(Station.builder()
                    .provider(identity.provider()).stationId(identity.stationId()).name(snapshot.stationName())
                    .location(snapshot.location()).createdAt(snapshot.collectedAt()).build()));
            station.update(snapshot.stationName(),snapshot.location(),snapshot.collectedAt());
            if (existing == null) {
                Charger created = chargerRepository.save(Charger.builder().id(snapshot.chargerId()).station(station)
                        .status(snapshot.status()).rawStatus(snapshot.rawStatus()).details(snapshot.details())
                        .sourceObservedAt(snapshot.sourceObservedAt()).collectedAt(snapshot.collectedAt()).build());
                chargers.put(snapshot.chargerId(),created);
                continue;
            }
            existing.update(snapshot.status(),snapshot.rawStatus(),snapshot.details(),snapshot.sourceObservedAt(),snapshot.collectedAt());
        }
    }

    @Override
    @Transactional
    public void upsert(StationSnapshot snapshot) {
        ChargerId chargerId = snapshot.chargerId();
        Charger existingCharger = chargerRepository.findByIdentity(chargerId).orElse(null);
        if (existingCharger != null && !existingCharger.acceptsObservation(snapshot.sourceObservedAt())) {
            return;
        }
        Station station = stationRepository.findByProviderAndStationId(chargerId.provider(), chargerId.stationId())
                .orElseGet(() -> stationRepository.save(Station.builder()
                        .provider(chargerId.provider()).stationId(chargerId.stationId())
                        .name(snapshot.stationName()).location(snapshot.location())
                        .createdAt(snapshot.collectedAt()).build()));
        station.update(snapshot.stationName(), snapshot.location(), snapshot.collectedAt());
        if (existingCharger == null) {
            chargerRepository.save(Charger.builder().id(chargerId).station(station)
                    .status(snapshot.status()).rawStatus(snapshot.rawStatus()).details(snapshot.details())
                    .sourceObservedAt(snapshot.sourceObservedAt()).collectedAt(snapshot.collectedAt()).build());
            return;
        }
        existingCharger.update(snapshot.status(), snapshot.rawStatus(), snapshot.details(),
                snapshot.sourceObservedAt(), snapshot.collectedAt());
    }
    private record StationIdentity(String provider, String stationId) { }
}
