package com.plugpass.recommendation.service;

import com.plugpass.recommendation.domain.CandidateGroups;
import com.plugpass.recommendation.domain.RankedCandidate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.plugpass.ingestion.dto.StationSnapshot;
import com.plugpass.ingestion.service.StationUpsertService;
import com.plugpass.station.domain.Connector;
import com.plugpass.search.domain.StationSearchQuery;
import com.plugpass.station.domain.ChargerDetails;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.domain.GeoPoint;
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
@Import(RecommendationServiceTests.TimeBoundary.class)
class RecommendationServiceTests {
    @Autowired private RecommendationService recommendationService;
    @Autowired private StationUpsertService stationUpsertService;
    @Autowired private StationRepository stationRepository;
    @Autowired private ChargerRepository chargerRepository;
    private final Instant now = Instant.parse("2026-10-05T00:00:00Z");
    @AfterEach
    void deleteOwnedData() { chargerRepository.deleteAllInBatch(); stationRepository.deleteAllInBatch(); }
    @Test
    @DisplayName("검색 limit 전에 제한·최신성을 판단하여 먼 유효 후보를 놓치지 않는다")
    void ranksBeforeLimit() {
        saveCharger("restricted",0,"04",ChargerStatus.AVAILABLE,"Y",now);
        saveCharger("unverified",0.001,"04",ChargerStatus.AVAILABLE,"N",null);
        saveCharger("recent",0.002,"05",ChargerStatus.AVAILABLE,"N",now);
        CandidateGroups groups = recommendationService.recommend(new StationSearchQuery(new GeoPoint(0,0),1000,Connector.DC_COMBO,1),null);
        assertThat(groups.preferred()).extracting(RankedCandidate::name).containsExactly("recent");
        assertThat(groups.requiresConfirmation()).extracting(RankedCandidate::name).containsExactly("unverified");
        assertThat(groups.excluded()).extracting(RankedCandidate::name).containsExactly("restricted");
    }
    @Test
    @DisplayName("사용자가 제외한 충전소는 어느 그룹에도 나오지 않는다")
    void removesExcludedStationBeforeLimit() {
        saveCharger("first",0,"04",ChargerStatus.AVAILABLE,"N",now);
        saveCharger("second",0.001,"04",ChargerStatus.AVAILABLE,"N",now);
        Long excludeId = stationRepository.findByProviderAndStationId("ME","first").orElseThrow().getDatabaseId();
        CandidateGroups groups = recommendationService.recommend(new StationSearchQuery(new GeoPoint(0,0),1000,Connector.DC_COMBO,1),excludeId);
        assertThat(groups.preferred()).extracting(RankedCandidate::name).containsExactly("second");
        assertThat(groups.requiresConfirmation()).isEmpty();
        assertThat(groups.excluded()).isEmpty();
    }
    @Test
    @DisplayName("반경 밖과 미호환 충전소는 추천에 포함하지 않는다")
    void excludesOutsideAndIncompatible() {
        saveCharger("outside",0.02,"04",ChargerStatus.AVAILABLE,"N",now);
        saveCharger("incompatible",0,"02",ChargerStatus.AVAILABLE,"N",now);
        saveCharger("inside",0.001,"04",ChargerStatus.AVAILABLE,"N",now);
        CandidateGroups groups = recommendationService.recommend(new StationSearchQuery(new GeoPoint(0,0),1000,Connector.DC_COMBO,20),null);
        assertThat(groups.preferred()).extracting(RankedCandidate::name).containsExactly("inside");
        assertThat(groups.requiresConfirmation()).isEmpty();
        assertThat(groups.excluded()).isEmpty();
    }
    private void saveCharger(String stationId, double longitude, String connector, ChargerStatus status, String restriction, Instant observedAt) {
        ChargerDetails details = new ChargerDetails(connector,"24시간",restriction,null,null,null,null,null,null);
        stationUpsertService.upsert(new StationSnapshot(new ChargerId("ME",stationId,"01"),stationId,new GeoPoint(0,longitude),status,"2",details,observedAt,now));
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class TimeBoundary {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-10-05T00:00:00Z"),ZoneOffset.UTC); }
    }
}
