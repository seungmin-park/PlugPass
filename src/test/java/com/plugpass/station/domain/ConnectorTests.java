package com.plugpass.station.domain;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
class ConnectorTests {
    @ParameterizedTest
    @DisplayName("공급자 커넥터 조합을 호환 종류로 해석하며 미지원 코드는 일치하지 않는다")
    @MethodSource("connectorCodes")
    void matchesProviderCombination(String code, Set<Connector> expected) {
        assertThat(Stream.of(Connector.values()).filter(connector -> connector.matches(code)).toList()).containsExactlyInAnyOrderElementsOf(expected);
    }
    static Stream<Arguments> connectorCodes() {
        return Stream.of(Arguments.of("01",Set.of(Connector.DC_CHADEMO)), Arguments.of("02",Set.of(Connector.AC_SLOW)),
                Arguments.of("03",Set.of(Connector.DC_CHADEMO,Connector.AC_THREE_PHASE)), Arguments.of("04",Set.of(Connector.DC_COMBO)),
                Arguments.of("05",Set.of(Connector.DC_CHADEMO,Connector.DC_COMBO)), Arguments.of("06",Set.of(Connector.DC_CHADEMO,Connector.AC_THREE_PHASE,Connector.DC_COMBO)),
                Arguments.of("07",Set.of(Connector.AC_THREE_PHASE)), Arguments.of("08",Set.of(Connector.DC_COMBO)),
                Arguments.of("09",Set.of(Connector.NACS)), Arguments.of("10",Set.of(Connector.DC_COMBO,Connector.NACS)),
                Arguments.of(null,Set.of()), Arguments.of("unknown",Set.of()));
    }
}
