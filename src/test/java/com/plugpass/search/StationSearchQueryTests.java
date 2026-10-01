package com.plugpass.search;

import java.util.stream.Stream;
import com.plugpass.station.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StationSearchQueryTests {
    @ParameterizedTest
    @DisplayName("HTTP를 우회해도 검색 위치·커넥터·반경·limit의 불변조건을 지킨다")
    @MethodSource("invalidQueries")
    void rejectsInvalidQuery(GeoPoint location, int radius, Connector connector, int limit, String message) {
        assertThatThrownBy(() -> new StationSearchQuery(location,radius,connector,limit)).isInstanceOf(IllegalArgumentException.class).hasMessage(message);
    }
    static Stream<Arguments> invalidQueries() {
        GeoPoint location = new GeoPoint(37.5,126.6);
        return Stream.of(Arguments.of(null,100,Connector.DC_COMBO,20,"location must not be null"),
                Arguments.of(location,100,null,20,"connector must not be null"),
                Arguments.of(location,99,Connector.DC_COMBO,20,"radiusMeters must be between 100 and 10000"),
                Arguments.of(location,10001,Connector.DC_COMBO,20,"radiusMeters must be between 100 and 10000"),
                Arguments.of(location,100,Connector.DC_COMBO,0,"limit must be between 1 and 50"),
                Arguments.of(location,100,Connector.DC_COMBO,51,"limit must be between 1 and 50"));
    }
}
