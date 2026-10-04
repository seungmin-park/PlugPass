package com.plugpass.freshness.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import com.plugpass.ingestion.dto.StationSnapshot;
import com.plugpass.station.domain.ChargerDetails;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.domain.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FreshnessPolicyTests {
    @ParameterizedTest
    @DisplayName("관측 시각만으로 나노초까지 최신성 경계를 판정한다")
    @MethodSource("observationCases")
    void evaluatesObservation(Instant observedAt, Freshness expected, String reason) {
        Clock clock = Clock.fixed(Instant.parse("2026-10-02T00:00:00Z"), ZoneOffset.UTC);
        FreshnessPolicy policy = new FreshnessPolicy(Duration.ofMinutes(10));
        assertThat(policy.evaluate(observedAt, clock.instant())).isEqualTo(expected);
        assertThat(policy.assess(observedAt, clock.instant())).isEqualTo(new FreshnessAssessment(expected, reason));
    }
    static Stream<Arguments> observationCases() {
        Instant now = Instant.parse("2026-10-02T00:00:00Z");
        return Stream.of(Arguments.of(now,Freshness.RECENT,"WITHIN_MAX_AGE"),
                Arguments.of(now.minusSeconds(600).plusNanos(1),Freshness.RECENT,"WITHIN_MAX_AGE"),
                Arguments.of(now.minusSeconds(600),Freshness.RECENT,"WITHIN_MAX_AGE"),
                Arguments.of(now.minusSeconds(600).minusNanos(1),Freshness.STALE,"MAX_AGE_EXCEEDED"),
                Arguments.of(null,Freshness.UNVERIFIED,"SOURCE_OBSERVED_AT_MISSING"),
                Arguments.of(now.plusNanos(1),Freshness.UNVERIFIED,"SOURCE_OBSERVED_AT_IN_FUTURE"),
                Arguments.of(Instant.MIN,Freshness.STALE,"MAX_AGE_EXCEEDED"),
                Arguments.of(Instant.MAX,Freshness.UNVERIFIED,"SOURCE_OBSERVED_AT_IN_FUTURE"));
    }
    @ParameterizedTest
    @DisplayName("허용 시간은 양수만 받는다")
    @MethodSource("invalidMaxAges")
    void rejectsInvalidMaxAge(Duration maxAge) {
        assertThatThrownBy(() -> new FreshnessPolicy(maxAge)).isInstanceOf(IllegalArgumentException.class).hasMessage("maxAge must be positive");
    }
    static Stream<Arguments> invalidMaxAges() {
        return Stream.of(Arguments.of((Object)null), Arguments.of(Duration.ZERO), Arguments.of(Duration.ofNanos(-1)));
    }
    @Test
    @DisplayName("판정 기준 시각이 없으면 거부한다")
    void rejectsMissingNow() {
        FreshnessPolicy policy = new FreshnessPolicy(Duration.ofMinutes(10));
        assertThatThrownBy(() -> policy.evaluate(null,null)).isInstanceOf(IllegalArgumentException.class).hasMessage("now must not be null");
    }
    @Test
    @DisplayName("오래된 응답을 지금 재수집해도 관측 시각으로 STALE을 판정한다")
    void doesNotRefreshOldObservation() {
        Instant now = Instant.parse("2026-10-02T00:00:00Z");
        StationSnapshot snapshot = new StationSnapshot(new ChargerId("ME","one","01"),"충전소",new GeoPoint(37.5,126.6),ChargerStatus.AVAILABLE,"2",null,now.minusSeconds(3600),now);
        FreshnessPolicy policy = new FreshnessPolicy(Duration.ofMinutes(10));
        assertThat(policy.evaluate(snapshot.sourceObservedAt(),now)).isEqualTo(Freshness.STALE);
    }
    @Test
    @DisplayName("수집 시각과 원본 상태 변경 시각으로 누락된 관측 시각을 채우지 않는다")
    void doesNotSubstituteOtherTimes() {
        Instant now = Instant.parse("2026-10-02T00:00:00Z");
        ChargerDetails details = new ChargerDetails("04",null,null,null,null,"20261002090000",null,null,null);
        StationSnapshot snapshot = new StationSnapshot(new ChargerId("ME","one","01"),"충전소",new GeoPoint(37.5,126.6),ChargerStatus.AVAILABLE,"2",details,null,now);
        FreshnessPolicy policy = new FreshnessPolicy(Duration.ofMinutes(10));
        assertThat(policy.assess(snapshot.sourceObservedAt(),now)).isEqualTo(new FreshnessAssessment(Freshness.UNVERIFIED,"SOURCE_OBSERVED_AT_MISSING"));
    }
}
