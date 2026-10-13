package com.plugpass.station.domain;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChargerStatusChangeTests {
    @Test
    @DisplayName("새 상태는 커넥터·운영시간·폐쇄정보를 보존하고 관측 시각을 꾸미지 않는다")
    void updatesOnlyStatusFields() {
        Instant received = Instant.parse("2026-10-13T00:00:00Z");
        Station station = Station.builder().provider("ME").stationId("001").name("서울 충전소")
                .location(new GeoPoint(37.5,127)).createdAt(received).build();
        ChargerDetails details = new ChargerDetails("04", "24시간", "N", null, "지하", "20261013080000", null, null, null, "N", null);
        Charger charger = Charger.builder().id(new ChargerId("ME","001","01")).station(station)
                .status(ChargerStatus.AVAILABLE).rawStatus("2").details(details).collectedAt(received).build();
        charger.updateStatus(new ChargerStatusChange(ChargerStatus.OCCUPIED, "3", "20261013090000", "20261013090000", null, "20261013090000", received.plusSeconds(10)));
        assertThat(charger.getStatus()).isEqualTo(ChargerStatus.OCCUPIED);
        assertThat(charger.getDetails()).isEqualTo(new ChargerDetails("04","24시간","N",null,"지하","20261013090000","20261013090000",null,"20261013090000","N",null));
        assertThat(charger.getCollectedAt()).isEqualTo(received.plusSeconds(10));
        assertThat(charger.getCreatedAt()).isEqualTo(received);
        assertThat(charger.getSourceObservedAt()).isNull();
    }
    @Test
    @DisplayName("늦게 도착한 이전 상태는 상태·상세·수집 시각을 바꾸지 않는다")
    void preservesNewerChange() {
        Instant received = Instant.parse("2026-10-13T00:00:00Z");
        Station station = Station.builder().provider("ME").stationId("001").name("서울 충전소")
                .location(new GeoPoint(37.5,127)).createdAt(received).build();
        ChargerDetails details = new ChargerDetails("04", "24시간", "N", null, null, "20261013090000", null, null, null);
        Charger charger = Charger.builder().id(new ChargerId("ME","001","01")).station(station)
                .status(ChargerStatus.AVAILABLE).rawStatus("2").details(details).collectedAt(received).build();
        charger.updateStatus(new ChargerStatusChange(ChargerStatus.OCCUPIED, "3", "20261013080000", null, null, null, received.plusSeconds(10)));
        assertThat(charger.getStatus()).isEqualTo(ChargerStatus.AVAILABLE);
        assertThat(charger.getDetails()).isEqualTo(details);
        assertThat(charger.getCollectedAt()).isEqualTo(received);
        assertThat(charger.getUpdatedAt()).isEqualTo(received);
    }
    @Test
    @DisplayName("잘못된 공급자 상태 변경 시각은 비교 가능한 값으로 수용하지 않는다")
    void rejectsInvalidChangeTimestamp() {
        assertThatThrownBy(() -> new ChargerStatusChange(ChargerStatus.OCCUPIED,"3","20261313090000",null,null,null,Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("status change time must be a valid yyyyMMddHHmmss value");
    }
    @Test
    @DisplayName("기본 정보의 이전 상태는 최신 상태를 보존하면서 운영 정보만 갱신한다")
    void catalogRefreshPreservesNewerStatus() {
        Instant received = Instant.parse("2026-10-13T00:00:00Z");
        Station station = Station.builder().provider("ME").stationId("001").name("서울 충전소")
                .location(new GeoPoint(37.5,127)).createdAt(received).build();
        ChargerDetails current = new ChargerDetails("04", "24시간", "N", null, null, "20261013090000", null, null, "20261013090000");
        Charger charger = Charger.builder().id(new ChargerId("ME","001","01")).station(station)
                .status(ChargerStatus.OCCUPIED).rawStatus("3").details(current).collectedAt(received).build();
        ChargerDetails catalog = new ChargerDetails("04", "09~18시", "Y", "입주민", null, "20261013080000", null, null, null);
        charger.update(ChargerStatus.AVAILABLE, "2", catalog, null, received.plusSeconds(10));
        assertThat(charger.getStatus()).isEqualTo(ChargerStatus.OCCUPIED);
        assertThat(charger.getRawStatus()).isEqualTo("3");
        assertThat(charger.getDetails().useTime()).isEqualTo("09~18시");
        assertThat(charger.getDetails().limitYn()).isEqualTo("Y");
        assertThat(charger.getDetails().sourceStatusChangedAtRaw()).isEqualTo("20261013090000");
        assertThat(charger.getDetails().chargingStartedAtRaw()).isEqualTo("20261013090000");
        assertThat(charger.getCollectedAt()).isEqualTo(received);
    }

}
