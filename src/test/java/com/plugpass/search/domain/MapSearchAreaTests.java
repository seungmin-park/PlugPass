package com.plugpass.search.domain;

import com.plugpass.station.domain.GeoPoint;
import com.plugpass.station.domain.Connector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class MapSearchAreaTests {
    @Test
    @DisplayName("유효 영역은 중심 좌표를 계산하고 조회 개수의 양 끝을 허용한다")
    void calculatesCenter() {
        MapSearchArea area = new MapSearchArea(37.49, 37.51, 126.99, 127.01);
        assertThat(area.center()).isEqualTo(new GeoPoint(37.5, 127));
        assertThat(new StationAreaSearchQuery(area, Connector.DC_COMBO, 1).limit()).isEqualTo(1);
        assertThat(new StationAreaSearchQuery(area, Connector.DC_COMBO, 50).limit()).isEqualTo(50);
    }
    @ParameterizedTest
    @DisplayName("뒤집히거나 너무 넓거나 유한하지 않은 지도 경계는 도메인에서도 거부한다")
    @CsvSource({"37,37,126,127", "38,37,126,127", "37,37.01,127,126", "37,37.01,127,127", "37,38,126,127", "NaN,37,126,127", "37,37.01,126,Infinity", "-91,-89.99,126,126.01", "37,37.01,179,-179"})
    void rejectsInvalidArea(double south, double north, double west, double east) {
        assertThatThrownBy(() -> new MapSearchArea(south, north, west, east)).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest
    @DisplayName("지도 조회 개수 제한 바로 밖은 거부한다")
    @ValueSource(ints = {0, 51})
    void rejectsInvalidLimit(int limit) {
        MapSearchArea area = new MapSearchArea(37.49, 37.51, 126.99, 127.01);
        assertThatThrownBy(() -> new StationAreaSearchQuery(area, Connector.DC_COMBO, limit)).isInstanceOf(IllegalArgumentException.class);
    }
}
