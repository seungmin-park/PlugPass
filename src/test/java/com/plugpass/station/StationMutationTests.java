package com.plugpass.station;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StationMutationTests {
    @Test
    @DisplayName("충전소 갱신은 생성 시각을 유지하고 수정 시각만 변경한다")
    void updatesStationWithExplicitTime() {
        Instant createdAt = Instant.parse("2026-10-01T00:00:00Z");
        Station station = Station.builder().provider("ME").stationId("28260005").name("기존 이름").location(new GeoPoint(37.5,126.6)).createdAt(createdAt).build();

        station.update("새 이름", new GeoPoint(37.6,126.7), createdAt.plusSeconds(60));

        assertThat(station.getName()).isEqualTo("새 이름");
        assertThat(station.getLocation()).isEqualTo(new GeoPoint(37.6,126.7));
        assertThat(station.getCreatedAt()).isEqualTo(createdAt);
        assertThat(station.getUpdatedAt()).isEqualTo(createdAt.plusSeconds(60));
    }
    @Test
    @DisplayName("충전소 갱신 검증 실패 시 이름·위치·시각을 모두 유지한다")
    void preservesStationAfterInvalidUpdate() {
        Instant createdAt = Instant.parse("2026-10-01T00:00:00Z");
        Station station = Station.builder().provider("ME").stationId("28260005").name("기존 이름").location(new GeoPoint(37.5,126.6)).createdAt(createdAt).build();

        assertThatThrownBy(() -> station.update("새 이름", null, createdAt.plusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("location must not be null");

        assertThat(station.getName()).isEqualTo("기존 이름");
        assertThat(station.getLocation()).isEqualTo(new GeoPoint(37.5,126.6));
        assertThat(station.getCreatedAt()).isEqualTo(createdAt);
        assertThat(station.getUpdatedAt()).isEqualTo(createdAt);
    }
    @Test
    @DisplayName("충전기 갱신 실패 시 상태·원본·수집·수정 시각을 모두 유지한다")
    void preservesChargerAfterInvalidUpdate() {
        Instant collectedAt = Instant.parse("2026-10-01T00:00:00Z");
        Station station = Station.builder().provider("ME").stationId("28260005").name("충전소").location(new GeoPoint(37.5,126.6)).createdAt(collectedAt).build();
        Charger charger = Charger.builder().id(new ChargerId("ME", "28260005", "02")).station(station).status(ChargerStatus.AVAILABLE).rawStatus("2").collectedAt(collectedAt).build();

        assertThatThrownBy(() -> charger.update(ChargerStatus.OCCUPIED, "3", null, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("collectedAt must not be null");

        assertThat(charger.getStatus()).isEqualTo(ChargerStatus.AVAILABLE);
        assertThat(charger.getRawStatus()).isEqualTo("2");
        assertThat(charger.getCreatedAt()).isEqualTo(collectedAt);
        assertThat(charger.getUpdatedAt()).isEqualTo(collectedAt);
        assertThat(charger.getCollectedAt()).isEqualTo(collectedAt);
    }
    @Test
    @DisplayName("충전기는 다른 충전소에 속한 식별자를 거부한다")
    void rejectsMismatchedStationIdentity() {
        Instant collectedAt = Instant.parse("2026-10-01T00:00:00Z");
        Station station = Station.builder().provider("ME").stationId("28260005").name("충전소").location(new GeoPoint(37.5,126.6)).createdAt(collectedAt).build();

        assertThatThrownBy(() -> Charger.builder().id(new ChargerId("HE", "28260005", "02")).station(station).status(ChargerStatus.UNKNOWN).collectedAt(collectedAt).build())
                .isInstanceOf(IllegalArgumentException.class).hasMessage("charger identity must belong to station");
    }
}
