package com.plugpass.search.service;

import com.plugpass.search.dto.ChargerObservation;
import com.plugpass.search.dto.StationDetail;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.plugpass.exception.StationNotFoundException;
import com.plugpass.freshness.domain.Freshness;
import com.plugpass.ingestion.dto.StationSnapshot;
import com.plugpass.ingestion.service.StationUpsertService;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(StationDetailTests.TimeBoundary.class)
class StationDetailTests {
    @Autowired private StationQueryService stationQueryService;
    @Autowired private StationUpsertService stationUpsertService;
    @Autowired private StationRepository stationRepository;
    @Autowired private ChargerRepository chargerRepository;
    private final Instant now = Instant.parse("2026-10-02T00:00:00Z");
    @AfterEach
    void deleteOwnedData() { chargerRepository.deleteAllInBatch(); stationRepository.deleteAllInBatch(); }
    @Test
    @DisplayName("충전소 상세는 모든 충전기의 상태·시각·최신성·제한 근거를 번호순으로 반환한다")
    void returnsAllChargerEvidence() {
        ChargerDetails details = new ChargerDetails("04","24시간","Y","입주민 전용","출입 확인","20190829121020",null,null,null,"N",null);
        StationSnapshot occupied = new StationSnapshot(new ChargerId("ME","one","02"),"충전소",new GeoPoint(37.5,126.6),ChargerStatus.OCCUPIED,"3",details,now.minusSeconds(601),now);
        StationSnapshot available = new StationSnapshot(new ChargerId("ME","one","01"),"충전소",new GeoPoint(37.5,126.6),ChargerStatus.AVAILABLE,"2",details,now.minusSeconds(600),now);
        StationSnapshot unknown = new StationSnapshot(new ChargerId("ME","one","03"),"충전소",new GeoPoint(37.5,126.6),ChargerStatus.UNKNOWN,"9",null,null,now);
        stationUpsertService.upsert(occupied); stationUpsertService.upsert(available); stationUpsertService.upsert(unknown);
        Long stationId = stationRepository.findByProviderAndStationId("ME","one").orElseThrow().getDatabaseId();
        StationDetail detail = stationQueryService.detail(stationId);
        assertThat(detail).isNotNull();
        assertThat(detail.id()).isEqualTo(stationId);
        assertThat(detail.provider()).isEqualTo("ME");
        assertThat(detail.providerStationId()).isEqualTo("one");
        assertThat(detail.name()).isEqualTo("충전소");
        assertThat(detail.location()).isEqualTo(new GeoPoint(37.5,126.6));
        assertThat(detail.chargers()).extracting(ChargerObservation::chargerId).containsExactly("01","02","03");
        assertThat(detail.chargers()).extracting(ChargerObservation::status).containsExactly(ChargerStatus.AVAILABLE,ChargerStatus.OCCUPIED,ChargerStatus.UNKNOWN);
        assertThat(detail.chargers()).extracting(ChargerObservation::rawStatus).containsExactly("2","3","9");
        assertThat(detail.chargers()).extracting(charger -> charger.freshness().freshness()).containsExactly(Freshness.RECENT,Freshness.STALE,Freshness.UNVERIFIED);
        assertThat(detail.chargers()).extracting(charger -> charger.freshness().reasonCode()).containsExactly("WITHIN_MAX_AGE","MAX_AGE_EXCEEDED","SOURCE_OBSERVED_AT_MISSING");
        assertThat(detail.chargers()).extracting(ChargerObservation::sourceObservedAt).containsExactly(now.minusSeconds(600),now.minusSeconds(601),null);
        assertThat(detail.chargers()).extracting(ChargerObservation::collectedAt).containsExactly(now,now,now);
        assertThat(detail.chargers()).extracting(ChargerObservation::details).containsExactly(details,details,null);
    }
    @Test
    @DisplayName("없는 내부 충전소 ID는 공개 메시지를 가진 예외로 반환하고 데이터를 바꾸지 않는다")
    void rejectsMissingStation() {
        assertThatThrownBy(() -> stationQueryService.detail(Long.MAX_VALUE)).isInstanceOf(StationNotFoundException.class).hasMessage("충전소를 찾을 수 없습니다");
        assertThat(stationRepository.count()).isZero();
        assertThat(chargerRepository.count()).isZero();
    }
    @Test
    @DisplayName("공급자 충전소 ID가 같아도 내부 ID로 다른 공급자의 데이터를 구분한다")
    void distinguishesProviders() {
        stationUpsertService.upsert(new StationSnapshot(new ChargerId("ME","one","01"),"기관1",new GeoPoint(37.5,126.6),ChargerStatus.AVAILABLE,"2",null,null,now));
        stationUpsertService.upsert(new StationSnapshot(new ChargerId("HE","one","01"),"기관2",new GeoPoint(37.5,126.6),ChargerStatus.OCCUPIED,"3",null,null,now));
        Long stationId = stationRepository.findByProviderAndStationId("HE","one").orElseThrow().getDatabaseId();
        StationDetail detail = stationQueryService.detail(stationId);
        assertThat(detail).isNotNull();
        assertThat(detail.provider()).isEqualTo("HE");
        assertThat(detail.name()).isEqualTo("기관2");
        assertThat(detail.chargers()).extracting(ChargerObservation::status).containsExactly(ChargerStatus.OCCUPIED);
    }
    @Test
    @DisplayName("충전기가 없는 충전소도 상세는 빈 충전기 목록으로 반환한다")
    void returnsStationWithoutChargers() {
        Station station = stationRepository.save(Station.builder().provider("ME").stationId("empty").name("충전소").location(new GeoPoint(37.5,126.6)).createdAt(now).build());
        StationDetail detail = stationQueryService.detail(station.getDatabaseId());
        assertThat(detail).isNotNull();
        assertThat(detail.chargers()).isEmpty();
    }
    @Test
    @DisplayName("내부 충전소 ID가 null이면 조회 전에 거부한다")
    void rejectsNullStationId() {
        assertThatThrownBy(() -> stationQueryService.detail(null)).isInstanceOf(IllegalArgumentException.class).hasMessage("stationId must not be null");
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class TimeBoundary {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-10-02T00:00:00Z"),ZoneOffset.UTC); }
    }
}
