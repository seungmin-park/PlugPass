package com.plugpass.ingestion.service;

import com.plugpass.ingestion.dto.ChargerStatusSnapshot;
import com.plugpass.station.domain.Charger;
import com.plugpass.station.repository.ChargerRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.plugpass.station.domain.ChargerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultStatusUpsertService implements StatusUpsertService {
    private final ChargerRepository chargerRepository;
    public DefaultStatusUpsertService(ChargerRepository chargerRepository) { this.chargerRepository = chargerRepository; }
    @Override @Transactional
    public int applyPage(List<ChargerStatusSnapshot> snapshots) {
        if (snapshots.isEmpty()) { return 0; }
        Map<ChargerId,Charger> chargers = chargerRepository.findByIdentities(snapshots.stream().map(ChargerStatusSnapshot::chargerId).toList())
                .stream().collect(Collectors.toMap(Charger::getId,charger -> charger));
        int matched = 0;
        for (ChargerStatusSnapshot snapshot : snapshots) {
            Charger charger = chargers.get(snapshot.chargerId());
            if (charger == null) { continue; }
            charger.updateStatus(snapshot.change());
            matched++;
        }
        return matched;
    }
}
