package com.plugpass.station;

import java.time.Instant;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class StationPersistenceTests {
    @Autowired private StationRepository stationRepository;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("복합 식별자·위치·상태·원본 시각을 DB 매핑에서 복원한다")
    void restoresMappedValues() {
        Instant collectedAt = Instant.parse("2026-10-01T00:00:00Z");
        Station station = Station.builder().provider("ME").stationId("00260005").name("충전소").location(new GeoPoint(37.5,126.6)).createdAt(collectedAt).build();
        stationRepository.save(station);
        ChargerDetails details = new ChargerDetails("03", "24시간", "Y", "입주민", "공사", "20190829121020", "20210801121020", "20210801123020", "20210802131020");
        Charger charger = Charger.builder().id(new ChargerId("ME", "00260005", "02")).station(station).status(ChargerStatus.UNAVAILABLE).rawStatus("5").details(details).sourceObservedAt(null).collectedAt(collectedAt).build();
        chargerRepository.save(charger);
        // 값 객체/enum/관계가 실제 DB에서 복원되는지 확인하는 테스트이므로 flush/clear한다.
        entityManager.flush();
        entityManager.clear();

        Charger reloadedCharger = chargerRepository.findByIdentity(new ChargerId("ME", "00260005", "02")).orElseThrow();
        Station reloadedStation = stationRepository.findByProviderAndStationId("ME", "00260005").orElseThrow();

        assertThat(reloadedCharger.getId()).isEqualTo(new ChargerId("ME", "00260005", "02"));
        assertThat(reloadedCharger.getStatus()).isEqualTo(ChargerStatus.UNAVAILABLE);
        assertThat(reloadedCharger.getRawStatus()).isEqualTo("5");
        assertThat(reloadedCharger.getDetails()).isEqualTo(details);
        assertThat(reloadedCharger.getCreatedAt()).isEqualTo(collectedAt);
        assertThat(reloadedCharger.getUpdatedAt()).isEqualTo(collectedAt);
        assertThat(reloadedCharger.getSourceObservedAt()).isNull();
        assertThat(reloadedCharger.getStation().getDatabaseId()).isEqualTo(reloadedStation.getDatabaseId());
        assertThat(reloadedStation.getLocation()).isEqualTo(new GeoPoint(37.5,126.6));
        assertThat(entityManager.createNativeQuery("select status from charger").getSingleResult()).isEqualTo("UNAVAILABLE");
    }

    @Test
    @DisplayName("같은 기관·충전소의 중복 행을 DB 제약으로 거부한다")
    void rejectsDuplicateStation() {
        Instant createdAt = Instant.parse("2026-10-01T00:00:00Z");
        Station first = Station.builder().provider("ME").stationId("28260005").name("충전소").location(new GeoPoint(37.5,126.6)).createdAt(createdAt).build();
        Station duplicate = Station.builder().provider("ME").stationId("28260005").name("다른 이름").location(new GeoPoint(37.6,126.7)).createdAt(createdAt).build();
        stationRepository.save(first);

        assertThatThrownBy(() -> stationRepository.save(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 복합 충전기 식별자의 중복 행을 DB 제약으로 거부한다")
    void rejectsDuplicateCharger() {
        Instant collectedAt = Instant.parse("2026-10-01T00:00:00Z");
        Station station = Station.builder().provider("ME").stationId("28260005").name("충전소").location(new GeoPoint(37.5,126.6)).createdAt(collectedAt).build();
        stationRepository.save(station);
        Charger first = Charger.builder().id(new ChargerId("ME", "28260005", "02")).station(station).status(ChargerStatus.AVAILABLE).rawStatus("2").collectedAt(collectedAt).build();
        Charger duplicate = Charger.builder().id(new ChargerId("ME", "28260005", "02")).station(station).status(ChargerStatus.OCCUPIED).rawStatus("3").collectedAt(collectedAt).build();
        chargerRepository.save(first);

        assertThatThrownBy(() -> chargerRepository.save(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
