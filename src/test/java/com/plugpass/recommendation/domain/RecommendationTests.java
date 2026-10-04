package com.plugpass.recommendation.domain;

import java.util.List;
import java.util.stream.Stream;
import com.plugpass.freshness.domain.Freshness;
import com.plugpass.freshness.domain.FreshnessAssessment;
import com.plugpass.station.domain.Connector;
import com.plugpass.station.domain.ChargerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;

class RecommendationTests {
    private final CandidatePolicy candidatePolicy = new CandidatePolicy();

    @Test
    @DisplayName("최신 이용 가능 후보는 우선 그룹에 두고 거리와 ID로 정렬한다")
    void ranksRecentAvailable() {
        CandidateCharger charger = charger("04", ChargerStatus.AVAILABLE, Freshness.RECENT, "N", "24시간", null);
        StationCandidate farther = new StationCandidate(3L,"먼 곳",200,Connector.DC_COMBO,List.of(charger));
        StationCandidate second = new StationCandidate(2L,"동률 둘",100,Connector.DC_COMBO,List.of(charger));
        StationCandidate first = new StationCandidate(1L,"동률 하나",100,Connector.DC_COMBO,List.of(charger));
        CandidateGroups groups = candidatePolicy.rank(List.of(farther,second,first));
        assertThat(groups.preferred()).extracting(RankedCandidate::id).containsExactly(1L,2L,3L);
        assertThat(groups.preferred().getFirst().reasonCodes()).containsExactly("RECENT_AVAILABLE");
        assertThat(groups.requiresConfirmation()).isEmpty();
        assertThat(groups.excluded()).isEmpty();
    }
    @Test
    @DisplayName("관측 시각이 없는 이용 가능 보고는 확인 필요 그룹에 둔다")
    void separatesUnverified() {
        CandidateCharger charger = new CandidateCharger("04",ChargerStatus.AVAILABLE,
                new FreshnessAssessment(Freshness.UNVERIFIED,"SOURCE_OBSERVED_AT_MISSING"),"N","24시간",null);
        CandidateGroups groups = candidatePolicy.rank(List.of(new StationCandidate(1L,"확인 필요",100,Connector.DC_COMBO,List.of(charger))));
        assertThat(groups.preferred()).isEmpty();
        assertThat(groups.requiresConfirmation()).extracting(RankedCandidate::id).containsExactly(1L);
        assertThat(groups.requiresConfirmation().getFirst().reasonCodes()).containsExactly("UNVERIFIED_AVAILABLE","SOURCE_OBSERVED_AT_MISSING");
    }
    @ParameterizedTest
    @DisplayName("이용 불가·미호환·삭제·오래된 상태는 이유와 함께 제외한다")
    @MethodSource("excludedChargers")
    void explainsExclusion(String connector, ChargerStatus status, Freshness freshness, String restriction, String deleted, String reason) {
        CandidateCharger charger = charger(connector,status,freshness,restriction,"24시간",deleted);
        CandidateGroups groups = candidatePolicy.rank(List.of(new StationCandidate(1L,"제외",100,Connector.DC_COMBO,List.of(charger))));
        assertThat(groups.preferred()).isEmpty();
        assertThat(groups.requiresConfirmation()).isEmpty();
        assertThat(groups.excluded()).extracting(RankedCandidate::id).containsExactly(1L);
        assertThat(groups.excluded().getFirst().reasonCodes()).contains(reason);
    }
    static Stream<Arguments> excludedChargers() {
        return Stream.of(Arguments.of("02",ChargerStatus.AVAILABLE,Freshness.RECENT,"N",null,"INCOMPATIBLE_CONNECTOR"),
                Arguments.of("04",ChargerStatus.AVAILABLE,Freshness.RECENT,"Y",null,"ACCESS_RESTRICTED"),
                Arguments.of("04",ChargerStatus.AVAILABLE,Freshness.RECENT,"N","Y","DELETED_BY_PROVIDER"),
                Arguments.of("04",ChargerStatus.AVAILABLE,Freshness.STALE,"N",null,"MAX_AGE_EXCEEDED"),
                Arguments.of("04",ChargerStatus.UNKNOWN,Freshness.RECENT,"N",null,"STATUS_UNKNOWN"),
                Arguments.of("04",ChargerStatus.OCCUPIED,Freshness.RECENT,"N",null,"NOT_AVAILABLE"),
                Arguments.of("04",ChargerStatus.UNAVAILABLE,Freshness.RECENT,"N",null,"NOT_AVAILABLE"));
    }
    @Test
    @DisplayName("미확인 이용 조건과 운영 시간은 우선 후보에도 경고로 남긴다")
    void warnsAboutMissingConditions() {
        CandidateCharger charger = charger("04",ChargerStatus.AVAILABLE,Freshness.RECENT,null,null,null);
        CandidateGroups groups = candidatePolicy.rank(List.of(new StationCandidate(1L,"미확인 조건",100,Connector.DC_COMBO,List.of(charger))));
        assertThat(groups.preferred().getFirst().reasonCodes()).containsExactly("RECENT_AVAILABLE","ACCESS_CONDITIONS_UNVERIFIED","OPERATING_HOURS_UNVERIFIED");
    }
    @Test
    @DisplayName("해석하지 않은 운영 시간은 현장 확인 이유를 남긴다")
    void warnsAboutLimitedHours() {
        CandidateCharger charger = charger("04",ChargerStatus.AVAILABLE,Freshness.RECENT,"N","09:00~18:00",null);
        CandidateGroups groups = candidatePolicy.rank(List.of(new StationCandidate(1L,"운영 시간",100,Connector.DC_COMBO,List.of(charger))));
        assertThat(groups.preferred().getFirst().reasonCodes()).containsExactly("RECENT_AVAILABLE","OPERATING_HOURS_REQUIRE_CHECK");
    }
    @Test
    @DisplayName("한 충전기의 최신성과 다른 충전기의 이용 가능 상태를 합치지 않는다")
    void keepsEvidenceOnSameCharger() {
        CandidateCharger occupied = charger("04",ChargerStatus.OCCUPIED,Freshness.RECENT,"N","24시간",null);
        CandidateCharger available = charger("04",ChargerStatus.AVAILABLE,Freshness.UNVERIFIED,"N","24시간",null);
        CandidateGroups groups = candidatePolicy.rank(List.of(new StationCandidate(1L,"혼합 상태",100,Connector.DC_COMBO,List.of(occupied,available))));
        assertThat(groups.preferred()).isEmpty();
        assertThat(groups.requiresConfirmation()).hasSize(1);
    }
    @Test
    @DisplayName("제한 없는 호환 충전기가 하나 있으면 제한 충전기의 이유를 섞지 않는다")
    void choosesEligibleCharger() {
        CandidateCharger restricted = charger("04",ChargerStatus.AVAILABLE,Freshness.RECENT,"Y",null,null);
        CandidateCharger eligible = charger("05",ChargerStatus.AVAILABLE,Freshness.RECENT,"N","24시간",null);
        CandidateGroups groups = candidatePolicy.rank(List.of(new StationCandidate(1L,"혼합 조건",100,Connector.DC_COMBO,List.of(restricted,eligible))));
        assertThat(groups.preferred().getFirst().reasonCodes()).containsExactly("RECENT_AVAILABLE");
        assertThat(groups.excluded()).isEmpty();
    }
    @Test
    @DisplayName("후보가 없으면 모든 그룹이 비어 있다")
    void returnsEmpty() {
        CandidateGroups groups = candidatePolicy.rank(List.of());
        assertThat(groups.preferred()).isEmpty();
        assertThat(groups.requiresConfirmation()).isEmpty();
        assertThat(groups.excluded()).isEmpty();
    }
    private CandidateCharger charger(String connector, ChargerStatus status, Freshness freshness, String restriction, String hours, String deleted) {
        String reason = freshness == Freshness.STALE ? "MAX_AGE_EXCEEDED" : "WITHIN_MAX_AGE";
        return new CandidateCharger(connector,status,new FreshnessAssessment(freshness,reason),restriction,hours,deleted);
    }
}
