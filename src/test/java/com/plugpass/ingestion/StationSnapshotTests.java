package com.plugpass.ingestion;

import java.time.Instant;
import com.plugpass.station.ChargerId;
import com.plugpass.station.ChargerStatus;
import com.plugpass.station.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StationSnapshotTests {
    @Test
    @DisplayName("정규화 입력은 누락된 충전기 식별자를 거부한다")
    void rejectsMissingIdentity() {
        assertThatThrownBy(() -> new StationSnapshot(null, "충전소", new GeoPoint(37.5,126.6), ChargerStatus.UNKNOWN, null, null, null, Instant.parse("2026-10-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("chargerId must not be null");
    }
    @ParameterizedTest
    @DisplayName("정규화 입력은 빈 충전소 이름을 거부한다")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsBlankName(String stationName) {
        assertThatThrownBy(() -> new StationSnapshot(new ChargerId("ME","28260005","02"), stationName, new GeoPoint(37.5,126.6), ChargerStatus.UNKNOWN, null, null, null, Instant.parse("2026-10-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("stationName must not be blank");
    }
    @Test
    @DisplayName("정규화 입력은 누락된 위치를 거부한다")
    void rejectsMissingLocation() {
        assertThatThrownBy(() -> new StationSnapshot(new ChargerId("ME","28260005","02"), "충전소", null, ChargerStatus.UNKNOWN, null, null, null, Instant.parse("2026-10-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("location must not be null");
    }
    @Test
    @DisplayName("정규화 입력은 누락된 업무 상태를 거부한다")
    void rejectsMissingStatus() {
        assertThatThrownBy(() -> new StationSnapshot(new ChargerId("ME","28260005","02"), "충전소", new GeoPoint(37.5,126.6), null, null, null, null, Instant.parse("2026-10-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("status must not be null");
    }
    @Test
    @DisplayName("정규화 입력은 누락된 수집 시각을 거부한다")
    void rejectsMissingCollectedAt() {
        assertThatThrownBy(() -> new StationSnapshot(new ChargerId("ME","28260005","02"), "충전소", new GeoPoint(37.5,126.6), ChargerStatus.UNKNOWN, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("collectedAt must not be null");
    }
}
