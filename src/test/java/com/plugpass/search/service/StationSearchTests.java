package com.plugpass.search.service;

import com.plugpass.search.domain.StationSearchQuery;
import com.plugpass.search.dto.ChargerObservation;
import com.plugpass.search.dto.StationMatch;
import com.plugpass.search.dto.StationSearchResult;
import com.plugpass.station.domain.Connector;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import com.plugpass.ingestion.dto.StationSnapshot;
import com.plugpass.ingestion.service.StationUpsertService;
import com.plugpass.ingestion.domain.SyncRun;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.freshness.domain.Freshness;
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

@SpringBootTest
@Import(StationSearchTests.TimeBoundary.class)
class StationSearchTests {
    @Autowired private StationQueryService stationQueryService;
    @Autowired private StationUpsertService stationUpsertService;
    @Autowired private StationRepository stationRepository;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private SyncRunRepository syncRunRepository;
    private final Instant now = Instant.parse("2026-10-02T00:00:00Z");
    @AfterEach
    void deleteOwnedData() { chargerRepository.deleteAllInBatch(); stationRepository.deleteAllInBatch(); syncRunRepository.deleteAllInBatch(); }

    @Test
    @DisplayName("최초 전체 성공 전 빈 검색은 데이터 준비 중임을 알려준다")
    void reportsNotReady() {
        StationSearchResult result = stationQueryService.search(new StationSearchQuery(new GeoPoint(37.5,126.6),1000,Connector.DC_COMBO,20));
        assertThat(result).isNotNull();
        assertThat(result.dataReady()).isFalse();
        assertThat(result.lastSuccessfulRunAt()).isNull();
        assertThat(result.stations()).isEmpty();
    }
    @Test
    @DisplayName("전체 성공 이후 빈 검색은 준비 완료와 빈 목록을 함께 반환한다")
    void reportsReadyEmpty() {
        SyncRun run = SyncRun.builder().startedAt(now).build(); run.complete(0,null,null,now);
        syncRunRepository.save(run);
        StationSearchResult result = stationQueryService.search(new StationSearchQuery(new GeoPoint(37.5,126.6),1000,Connector.DC_COMBO,20));
        assertThat(result).isNotNull();
        assertThat(result.dataReady()).isTrue();
        assertThat(result.lastSuccessfulRunAt()).isEqualTo(now);
        assertThat(result.stations()).isEmpty();
    }
    @Test
    @DisplayName("호환 충전기만 상태와 최신성 근거를 합쳐 반환한다")
    void returnsCompatibleEvidence() {
        saveCharger("one","01",new GeoPoint(37.5,126.6),"05",ChargerStatus.AVAILABLE,null);
        saveCharger("one","02",new GeoPoint(37.5,126.6),"04",ChargerStatus.OCCUPIED,now.minusSeconds(601));
        saveCharger("one","03",new GeoPoint(37.5,126.6),"02",ChargerStatus.AVAILABLE,now);
        StationSearchResult result = stationQueryService.search(new StationSearchQuery(new GeoPoint(37.5,126.6),1000,Connector.DC_COMBO,20));
        assertThat(result).isNotNull();
        assertThat(result.stations()).hasSize(1);
        StationMatch station = result.stations().getFirst();
        assertThat(station.distanceMeters()).isZero();
        assertThat(station.chargers()).extracting(ChargerObservation::chargerId).containsExactly("01","02");
        assertThat(station.chargers()).extracting(ChargerObservation::status).containsExactly(ChargerStatus.AVAILABLE,ChargerStatus.OCCUPIED);
        assertThat(station.chargers()).extracting(observation -> observation.freshness().freshness()).containsExactly(Freshness.UNVERIFIED,Freshness.STALE);
        assertThat(station.chargers()).extracting(observation -> observation.freshness().reasonCode()).containsExactly("SOURCE_OBSERVED_AT_MISSING","MAX_AGE_EXCEEDED");
        assertThat(station.chargers().getFirst().collectedAt()).isEqualTo(now);
        assertThat(station.chargers().getFirst().details().limitYn()).isEqualTo("N");
    }
    @Test
    @DisplayName("거리가 같으면 내부 충전소 ID로 정렬하고 limit을 적용한다")
    void sortsTiesAndLimits() {
        saveCharger("z","01",new GeoPoint(37.5,126.6),"04",ChargerStatus.AVAILABLE,now);
        saveCharger("a","01",new GeoPoint(37.5,126.6),"04",ChargerStatus.AVAILABLE,now);
        Long firstId = stationRepository.findByProviderAndStationId("ME","z").orElseThrow().getDatabaseId();
        StationSearchResult result = stationQueryService.search(new StationSearchQuery(new GeoPoint(37.5,126.6),1000,Connector.DC_COMBO,1));
        assertThat(result).isNotNull();
        assertThat(result.stations()).extracting(StationMatch::id).containsExactly(firstId);
    }
    @Test
    @DisplayName("거리를 우선해 가까운 충전소부터 반환하며 미호환과 반경 밖은 제외한다")
    void sortsDistanceAndFilters() {
        saveCharger("far","01",new GeoPoint(0,0.004),"04",ChargerStatus.AVAILABLE,now);
        saveCharger("near","01",new GeoPoint(0,0.001),"04",ChargerStatus.AVAILABLE,now);
        saveCharger("outside","01",new GeoPoint(0,0.02),"04",ChargerStatus.AVAILABLE,now);
        saveCharger("wrong","01",new GeoPoint(0,0),"02",ChargerStatus.AVAILABLE,now);
        StationSearchResult result = stationQueryService.search(new StationSearchQuery(new GeoPoint(0,0),1000,Connector.DC_COMBO,20));
        assertThat(result).isNotNull();
        assertThat(result.stations()).extracting(StationMatch::providerStationId).containsExactly("near","far");
    }
    @Test
    @DisplayName("반경과 같은 거리는 포함하고 바로 밖의 충전소는 제외한다")
    void includesRadiusBoundary() {
        double boundaryLatitude = Math.toDegrees(100.0 / 6371008.8);
        saveCharger("boundary","01",new GeoPoint(boundaryLatitude,0),"04",ChargerStatus.AVAILABLE,now);
        saveCharger("outside","01",new GeoPoint(boundaryLatitude + 0.00000001,0),"04",ChargerStatus.AVAILABLE,now);
        StationSearchResult result = stationQueryService.search(new StationSearchQuery(new GeoPoint(0,0),100,Connector.DC_COMBO,20));
        assertThat(result).isNotNull();
        assertThat(result.stations()).extracting(StationMatch::providerStationId).containsExactly("boundary");
    }
    @Test
    @DisplayName("날짜 변경선 양쪽의 가까운 충전소도 검색한다")
    void searchesAcrossDateLine() {
        saveCharger("across","01",new GeoPoint(0,-179.999),"04",ChargerStatus.UNKNOWN,null);
        StationSearchResult result = stationQueryService.search(new StationSearchQuery(new GeoPoint(0,179.999),1000,Connector.DC_COMBO,20));
        assertThat(result).isNotNull();
        assertThat(result.stations()).extracting(StationMatch::providerStationId).containsExactly("across");
        assertThat(result.dataReady()).isFalse();
    }
    private void saveCharger(String stationId, String chargerId, GeoPoint location, String connectorCode, ChargerStatus status, Instant observedAt) {
        ChargerDetails details = new ChargerDetails(connectorCode,"24시간","N",null,null,null,null,null,null);
        stationUpsertService.upsert(new StationSnapshot(new ChargerId("ME",stationId,chargerId),"충전소",location,status,"2",details,observedAt,now));
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class TimeBoundary {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-10-02T00:00:00Z"),ZoneOffset.UTC); }
    }
}
