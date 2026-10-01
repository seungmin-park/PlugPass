package com.plugpass.ingestion;

import java.util.stream.Stream;
import com.plugpass.station.Charger;
import com.plugpass.station.ChargerId;
import com.plugpass.station.ChargerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderStatusMapperTests {
    @ParameterizedTest
    @DisplayName("공식 상태 코드를 업무 상태로 변환한다")
    @MethodSource("knownStatuses")
    void mapsOfficialStatus(String rawCode, ChargerStatus expected) {
        ProviderStatusMapper mapper = new ProviderStatusMapper();
        assertThat(mapper.map(rawCode)).isEqualTo(expected);
    }
    private static Stream<Arguments> knownStatuses() {
        return Stream.of(Arguments.of("0", ChargerStatus.UNKNOWN),
                Arguments.of("1", ChargerStatus.UNAVAILABLE),
                Arguments.of("2", ChargerStatus.AVAILABLE),
                Arguments.of("3", ChargerStatus.OCCUPIED),
                Arguments.of("4", ChargerStatus.UNAVAILABLE),
                Arguments.of("5", ChargerStatus.UNAVAILABLE));
    }
    @ParameterizedTest
    @DisplayName("누락되거나 미지원인 코드를 이용 가능으로 추측하지 않는다")
    @NullAndEmptySource
    @ValueSource(strings = {"9", "UNKNOWN", " ", " 2", "02"})
    void mapsUnsupportedStatusToUnknown(String rawCode) {
        ProviderStatusMapper mapper = new ProviderStatusMapper();
        assertThat(mapper.map(rawCode)).isEqualTo(ChargerStatus.UNKNOWN);
    }
    @ParameterizedTest
    @DisplayName("정규화한 뒤에도 충전기에 원본 상태 코드를 보존한다")
    @NullAndEmptySource
    @ValueSource(strings = {"2", "9", " 2"})
    void preservesRawStatus(String rawCode) {
        ProviderStatusMapper mapper = new ProviderStatusMapper();
        Charger charger = new Charger(new ChargerId("ME", "28260005", "02"), mapper.map(rawCode), rawCode);
        assertThat(charger.rawStatus()).isEqualTo(rawCode);
    }
    @Test
    @DisplayName("충전기는 누락된 업무 상태를 거부한다")
    void rejectsMissingNormalizedStatus() {
        assertThatThrownBy(() -> new Charger(new ChargerId("ME", "28260005", "02"), null, "2"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("status must not be null");
    }
}
