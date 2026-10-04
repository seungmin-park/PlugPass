package com.plugpass.ingestion.service;

import com.plugpass.ingestion.dto.StationSnapshot;

import java.time.Instant;
import com.plugpass.station.domain.Charger;
import com.plugpass.station.domain.ChargerDetails;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.domain.GeoPoint;
import com.plugpass.station.domain.Station;
import com.plugpass.station.repository.StationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class StationUpsertTests {
    @Autowired private StationUpsertService stationUpsertService;
    @Autowired private StationRepository stationRepository;
    @Autowired private ChargerRepository chargerRepository;

    @AfterEach
    void deleteCreatedData() {
        chargerRepository.deleteAllInBatch();
        stationRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("같은 충전기를 재수집하면 중복 없이 갱신하고 생성 시각을 유지한다")
    void updatesWithoutDuplicating() {
        ChargerId chargerId = new ChargerId("ME", "28260005", "02");
        Instant firstCollectedAt = Instant.parse("2026-10-01T00:00:00Z");
        Instant nextCollectedAt = firstCollectedAt.plusSeconds(60);
        ChargerDetails details = new ChargerDetails("03", "24시간", "N", null, null, "20190829121020", null, null, null);
        StationSnapshot first = new StationSnapshot(chargerId, "기존 이름", new GeoPoint(37.5, 126.6), ChargerStatus.AVAILABLE, "2", details, null, firstCollectedAt);
        StationSnapshot next = new StationSnapshot(chargerId, "새 이름", new GeoPoint(37.6, 126.7), ChargerStatus.OCCUPIED, "3", details, null, nextCollectedAt);

        stationUpsertService.upsert(first);
        stationUpsertService.upsert(next);

        assertThat(chargerRepository.findByIdentity(chargerId)).isPresent();
        Charger reloadedCharger = chargerRepository.findByIdentity(chargerId).orElseThrow();
        Station reloadedStation = stationRepository.findByProviderAndStationId("ME", "28260005").orElseThrow();
        assertThat(chargerRepository.count()).isEqualTo(1);
        assertThat(stationRepository.count()).isEqualTo(1);
        assertThat(reloadedCharger.getStatus()).isEqualTo(ChargerStatus.OCCUPIED);
        assertThat(reloadedCharger.getRawStatus()).isEqualTo("3");
        assertThat(reloadedCharger.getCreatedAt()).isEqualTo(firstCollectedAt);
        assertThat(reloadedCharger.getUpdatedAt()).isEqualTo(nextCollectedAt);
        assertThat(reloadedCharger.getCollectedAt()).isEqualTo(nextCollectedAt);
        assertThat(reloadedCharger.getSourceObservedAt()).isNull();
        assertThat(reloadedCharger.getDetails()).isEqualTo(details);
        assertThat(reloadedStation.getName()).isEqualTo("새 이름");
        assertThat(reloadedStation.getLocation()).isEqualTo(new GeoPoint(37.6, 126.7));
        assertThat(reloadedStation.getCreatedAt()).isEqualTo(firstCollectedAt);
        assertThat(reloadedStation.getUpdatedAt()).isEqualTo(nextCollectedAt);
    }

    @Test
    @DisplayName("같은 충전소의 다른 충전기는 부모를 중복 생성하지 않는다")
    void sharesStationAcrossChargers() {
        Instant collectedAt = Instant.parse("2026-10-01T00:00:00Z");
        StationSnapshot first = new StationSnapshot(new ChargerId("ME", "28260005", "01"), "충전소", new GeoPoint(37.5,126.6), ChargerStatus.AVAILABLE, "2", null, null, collectedAt);
        StationSnapshot second = new StationSnapshot(new ChargerId("ME", "28260005", "02"), "충전소", new GeoPoint(37.5,126.6), ChargerStatus.OCCUPIED, "3", null, null, collectedAt);

        stationUpsertService.upsert(first);
        stationUpsertService.upsert(second);

        assertThat(stationRepository.count()).isEqualTo(1);
        assertThat(chargerRepository.findAll()).extracting(Charger::getId)
                .containsExactlyInAnyOrder(new ChargerId("ME", "28260005", "01"), new ChargerId("ME", "28260005", "02"));
    }

    @Test
    @DisplayName("다른 기관·충전소의 같은 충전기 번호를 각각 저장한다")
    void persistsDistinctCompositeIdentities() {
        Instant collectedAt = Instant.parse("2026-10-01T00:00:00Z");
        StationSnapshot first = new StationSnapshot(new ChargerId("ME", "28260005", "02"), "충전소1", new GeoPoint(37.5,126.6), ChargerStatus.AVAILABLE, "2", null, null, collectedAt);
        StationSnapshot second = new StationSnapshot(new ChargerId("HE", "28260005", "02"), "충전소2", new GeoPoint(37.5,126.6), ChargerStatus.OCCUPIED, "3", null, null, collectedAt);
        StationSnapshot third = new StationSnapshot(new ChargerId("ME", "28260006", "02"), "충전소3", new GeoPoint(37.5,126.6), ChargerStatus.UNKNOWN, "9", null, null, collectedAt);

        stationUpsertService.upsert(first);
        stationUpsertService.upsert(second);
        stationUpsertService.upsert(third);

        assertThat(stationRepository.count()).isEqualTo(3);
        assertThat(chargerRepository.findAll()).extracting(Charger::getId).containsExactlyInAnyOrder(first.chargerId(), second.chargerId(), third.chargerId());
    }

    @Test
    @DisplayName("신뢰 가능한 관측 시각이 역전되면 충전기와 충전소 정보를 모두 유지한다")
    void rejectsEarlierObservation() {
        ChargerId chargerId = new ChargerId("ME", "28260005", "02");
        Instant observedAt = Instant.parse("2026-10-01T00:00:00Z");
        Instant collectedAt = observedAt.plusSeconds(60);
        StationSnapshot latest = new StationSnapshot(chargerId, "최신 이름", new GeoPoint(37.5,126.6), ChargerStatus.OCCUPIED, "3", null, observedAt, collectedAt);
        StationSnapshot earlier = new StationSnapshot(chargerId, "과거 이름", new GeoPoint(36,125), ChargerStatus.AVAILABLE, "2", null, observedAt.minusSeconds(1), collectedAt.plusSeconds(60));
        stationUpsertService.upsert(latest);

        stationUpsertService.upsert(earlier);

        assertThat(chargerRepository.findByIdentity(chargerId)).isPresent();
        Charger reloadedCharger = chargerRepository.findByIdentity(chargerId).orElseThrow();
        Station reloadedStation = stationRepository.findByProviderAndStationId("ME", "28260005").orElseThrow();
        assertThat(reloadedCharger.getStatus()).isEqualTo(ChargerStatus.OCCUPIED);
        assertThat(reloadedCharger.getRawStatus()).isEqualTo("3");
        assertThat(reloadedCharger.getSourceObservedAt()).isEqualTo(observedAt);
        assertThat(reloadedCharger.getCollectedAt()).isEqualTo(collectedAt);
        assertThat(reloadedCharger.getUpdatedAt()).isEqualTo(collectedAt);
        assertThat(reloadedStation.getName()).isEqualTo("최신 이름");
        assertThat(reloadedStation.getLocation()).isEqualTo(new GeoPoint(37.5,126.6));
        assertThat(reloadedStation.getUpdatedAt()).isEqualTo(collectedAt);
    }

    @Test
    @DisplayName("관측 시각이 없는 공급자는 상태 변경 시각으로 응답 순서를 추측하지 않는다")
    void keepsUnverifiedOrderingExplicit() {
        ChargerId chargerId = new ChargerId("ME", "28260005", "02");
        Instant collectedAt = Instant.parse("2026-10-01T00:00:00Z");
        ChargerDetails laterChange = new ChargerDetails("03", null, null, null, null, "20261001090000", null, null, null);
        ChargerDetails earlierChange = new ChargerDetails("03", null, null, null, null, "20190829121020", null, null, null);
        StationSnapshot first = new StationSnapshot(chargerId, "충전소", new GeoPoint(37.5,126.6), ChargerStatus.OCCUPIED, "3", laterChange, null, collectedAt);
        StationSnapshot next = new StationSnapshot(chargerId, "충전소", new GeoPoint(37.5,126.6), ChargerStatus.AVAILABLE, "2", earlierChange, null, collectedAt.plusSeconds(60));
        stationUpsertService.upsert(first);

        stationUpsertService.upsert(next);

        assertThat(chargerRepository.findByIdentity(chargerId)).isPresent();
        Charger reloadedCharger = chargerRepository.findByIdentity(chargerId).orElseThrow();
        assertThat(reloadedCharger.getStatus()).isEqualTo(ChargerStatus.AVAILABLE);
        assertThat(reloadedCharger.getSourceObservedAt()).isNull();
        assertThat(reloadedCharger.getDetails().sourceStatusChangedAtRaw()).isEqualTo("20190829121020");
    }

    @Test
    @DisplayName("같은 관측 시각의 재수집도 중복 없이 수신 시각을 갱신한다")
    void acceptsEqualObservation() {
        ChargerId chargerId = new ChargerId("ME", "28260005", "02");
        Instant observedAt = Instant.parse("2026-10-01T00:00:00Z");
        StationSnapshot first = new StationSnapshot(chargerId, "충전소", new GeoPoint(37.5,126.6), ChargerStatus.AVAILABLE, "2", null, observedAt, observedAt);
        StationSnapshot repeated = new StationSnapshot(chargerId, "충전소", new GeoPoint(37.5,126.6), ChargerStatus.AVAILABLE, "2", null, observedAt, observedAt.plusSeconds(60));
        stationUpsertService.upsert(first);

        stationUpsertService.upsert(repeated);

        assertThat(chargerRepository.count()).isEqualTo(1);
        assertThat(chargerRepository.findByIdentity(chargerId).orElseThrow().getCollectedAt()).isEqualTo(observedAt.plusSeconds(60));
    }

    @Test
    @DisplayName("충전기 저장에 실패하면 같은 트랜잭션의 충전소 생성도 롤백한다")
    void rollsBackStationWhenChargerInsertFails() {
        StationSnapshot snapshot = new StationSnapshot(new ChargerId("ME", "28260005", "02"), "충전소", new GeoPoint(37.5,126.6), ChargerStatus.UNKNOWN, "x".repeat(256), null, null, Instant.parse("2026-10-01T00:00:00Z"));

        assertThatThrownBy(() -> stationUpsertService.upsert(snapshot)).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(chargerRepository.count()).isZero();
        assertThat(stationRepository.count()).isZero();
    }
}
