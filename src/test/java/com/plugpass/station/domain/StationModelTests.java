package com.plugpass.station.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StationModelTests {

    @Test
    @DisplayName("기관이 다르면 같은 충전소·충전기 번호도 충돌하지 않는다")
    void distinguishesProviders() {
        Set<ChargerId> chargerIds = new HashSet<>();
        chargerIds.add(new ChargerId("ME", "28260005", "02"));
        chargerIds.add(new ChargerId("HE", "28260005", "02"));

        assertThat(chargerIds).hasSize(2);
    }

    @Test
    @DisplayName("충전소가 다르면 같은 충전기 번호도 충돌하지 않는다")
    void distinguishesStations() {
        Set<ChargerId> chargerIds = new HashSet<>();
        chargerIds.add(new ChargerId("ME", "28260005", "02"));
        chargerIds.add(new ChargerId("ME", "28260006", "02"));

        assertThat(chargerIds).hasSize(2);
    }

    @Test
    @DisplayName("충전기 번호가 다르면 같은 충전소에서도 별도로 식별한다")
    void distinguishesChargerNumbers() {
        Set<ChargerId> chargerIds = new HashSet<>();
        chargerIds.add(new ChargerId("ME", "28260005", "01"));
        chargerIds.add(new ChargerId("ME", "28260005", "02"));

        assertThat(chargerIds).hasSize(2);
    }

    @Test
    @DisplayName("같은 세 식별자는 중복으로 판정하고 선행 0을 보존한다")
    void identifiesDuplicatesWithoutLosingLeadingZeros() {
        ChargerId chargerId = new ChargerId("ME", "00260005", "02");
        Set<ChargerId> chargerIds = new HashSet<>();
        chargerIds.add(chargerId);
        chargerIds.add(new ChargerId("ME", "00260005", "02"));

        assertThat(chargerIds).hasSize(1);
        assertThat(chargerId.stationId()).isEqualTo("00260005");
        assertThat(chargerId.chargerId()).isEqualTo("02");
    }

    @ParameterizedTest
    @DisplayName("빈 기관 식별자를 거부한다")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void rejectsBlankProvider(String provider) {
        assertThatThrownBy(() -> new ChargerId(provider, "28260005", "02"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("provider must not be blank");
    }

    @ParameterizedTest
    @DisplayName("빈 충전소 식별자를 거부한다")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void rejectsBlankStationId(String stationId) {
        assertThatThrownBy(() -> new ChargerId("ME", stationId, "02"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("stationId must not be blank");
    }

    @ParameterizedTest
    @DisplayName("빈 충전기 식별자를 거부한다")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void rejectsBlankChargerId(String chargerId) {
        assertThatThrownBy(() -> new ChargerId("ME", "28260005", chargerId))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("chargerId must not be blank");
    }

    @ParameterizedTest
    @DisplayName("위도 최솟값·최댓값과 내부 좌표를 허용한다")
    @ValueSource(doubles = {-90, 0, 37.569620, 90})
    void acceptsLatitudeBoundary(double latitude) {
        GeoPoint location = new GeoPoint(latitude, 126.641973);

        assertThat(location.latitude()).isEqualTo(latitude);
    }

    @ParameterizedTest
    @DisplayName("경도 최솟값·최댓값과 내부 좌표를 허용한다")
    @ValueSource(doubles = {-180, 0, 126.641973, 180})
    void acceptsLongitudeBoundary(double longitude) {
        GeoPoint location = new GeoPoint(37.569620, longitude);

        assertThat(location.longitude()).isEqualTo(longitude);
    }

    @ParameterizedTest
    @DisplayName("범위 밖 위도와 유한하지 않은 위도를 거부한다")
    @MethodSource("invalidLatitudes")
    void rejectsInvalidLatitude(double latitude) {
        assertThatThrownBy(() -> new GeoPoint(latitude, 126.641973))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("latitude must be finite and between -90 and 90");
    }

    private static Stream<Double> invalidLatitudes() {
        return Stream.of(Math.nextDown(-90.0), Math.nextUp(90.0),
                Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @ParameterizedTest
    @DisplayName("범위 밖 경도와 유한하지 않은 경도를 거부한다")
    @MethodSource("invalidLongitudes")
    void rejectsInvalidLongitude(double longitude) {
        assertThatThrownBy(() -> new GeoPoint(37.569620, longitude))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("longitude must be finite and between -180 and 180");
    }

    private static Stream<Double> invalidLongitudes() {
        return Stream.of(Math.nextDown(-180.0), Math.nextUp(180.0),
                Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @ParameterizedTest
    @DisplayName("충전소는 빈 기관 식별자를 직접 거부한다")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void stationRejectsBlankProvider(String provider) {
        GeoPoint location = new GeoPoint(37.569620, 126.641973);

        assertThatThrownBy(() -> Station.builder().provider(provider).stationId("28260005").name("검증 충전소").location(location).createdAt(Instant.parse("2026-10-01T00:00:00Z")).build())
                .isInstanceOf(IllegalArgumentException.class).hasMessage("provider must not be blank");
    }

    @ParameterizedTest
    @DisplayName("충전소는 빈 충전소 식별자를 직접 거부한다")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void stationRejectsBlankStationId(String stationId) {
        GeoPoint location = new GeoPoint(37.569620, 126.641973);

        assertThatThrownBy(() -> Station.builder().provider("ME").stationId(stationId).name("검증 충전소").location(location).createdAt(Instant.parse("2026-10-01T00:00:00Z")).build())
                .isInstanceOf(IllegalArgumentException.class).hasMessage("stationId must not be blank");
    }

    @ParameterizedTest
    @DisplayName("충전소는 빈 이름을 거부한다")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void stationRejectsBlankName(String name) {
        GeoPoint location = new GeoPoint(37.569620, 126.641973);

        assertThatThrownBy(() -> Station.builder().provider("ME").stationId("28260005").name(name).location(location).createdAt(Instant.parse("2026-10-01T00:00:00Z")).build())
                .isInstanceOf(IllegalArgumentException.class).hasMessage("name must not be blank");
    }

    @Test
    @DisplayName("충전소는 누락된 위치를 거부한다")
    void stationRejectsMissingLocation() {
        assertThatThrownBy(() -> Station.builder().provider("ME").stationId("28260005").name("검증 충전소").location(null).createdAt(Instant.parse("2026-10-01T00:00:00Z")).build())
                .isInstanceOf(IllegalArgumentException.class).hasMessage("location must not be null");
    }

    @Test
    @DisplayName("충전기는 누락된 복합 식별자를 거부한다")
    void chargerRejectsMissingIdentity() {
        assertThatThrownBy(() -> Charger.builder().id(null).status(ChargerStatus.UNKNOWN).build())
                .isInstanceOf(IllegalArgumentException.class).hasMessage("id must not be null");
    }

    @Test
    @DisplayName("유효한 충전소와 충전기는 업무 식별자와 위치를 보존한다")
    void createsValidModels() {
        GeoPoint location = new GeoPoint(37.569620, 126.641973);
        Station station = Station.builder().provider("ME").stationId("28260005").name("검증 충전소").location(location).createdAt(Instant.parse("2026-10-01T00:00:00Z")).build();
        Charger charger = Charger.builder().id(new ChargerId("ME", "28260005", "02")).station(station).status(ChargerStatus.UNKNOWN).collectedAt(Instant.parse("2026-10-01T00:00:00Z")).build();

        assertThat(station.getProvider()).isEqualTo("ME");
        assertThat(station.getStationId()).isEqualTo("28260005");
        assertThat(station.getName()).isEqualTo("검증 충전소");
        assertThat(station.getLocation()).isEqualTo(new GeoPoint(37.569620, 126.641973));
        assertThat(charger.getId()).isEqualTo(new ChargerId("ME", "28260005", "02"));
    }
}
