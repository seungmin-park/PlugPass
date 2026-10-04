package com.plugpass.station.domain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
class GeoDistanceTests {
    @Test
    @DisplayName("같은 위치의 직선거리는 0이다")
    void measuresSameLocation() { assertThat(new GeoPoint(0,0).distanceMetersTo(new GeoPoint(0,0))).isZero(); }
    @Test
    @DisplayName("적도의 1도는 지구 평균 반지름에 근거한 거리다")
    void measuresEquatorialDegree() { assertThat(new GeoPoint(0,0).distanceMetersTo(new GeoPoint(0,1))).isCloseTo(111195.08,within(0.02)); }
    @Test
    @DisplayName("날짜 변경선을 건너는 가까운 위치의 거리를 계산한다")
    void crossesDateLine() { assertThat(new GeoPoint(0,179.999).distanceMetersTo(new GeoPoint(0,-179.999))).isCloseTo(222.39,within(0.02)); }
    @Test
    @DisplayName("대척점에서도 유한한 거리를 반환한다")
    void measuresAntipodes() { assertThat(new GeoPoint(0,0).distanceMetersTo(new GeoPoint(0,180))).isCloseTo(20015114.44,within(0.02)); }
}
