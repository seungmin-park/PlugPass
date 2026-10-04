package com.plugpass.ingestion.config;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class IngestionConfigurationTests {
    @ParameterizedTest
    @DisplayName("호출 한도와 유한 시간 범위를 벗어나는 수집 설정은 거부한다")
    @MethodSource("invalidBudgets")
    void rejectsInvalidBudget(int pages, int requests, int retries, Duration timeout, Duration delay) {
        assertThatThrownBy(() -> new IngestionProperties(pages,requests,retries,timeout,delay)).isInstanceOf(IllegalArgumentException.class);
    }
    static Stream<Arguments> invalidBudgets() {
        return Stream.of(Arguments.of(0,20,1,Duration.ofSeconds(120),Duration.ofSeconds(1)),
                Arguments.of(11,20,1,Duration.ofSeconds(120),Duration.ofSeconds(1)),
                Arguments.of(10,0,1,Duration.ofSeconds(120),Duration.ofSeconds(1)),
                Arguments.of(10,21,1,Duration.ofSeconds(120),Duration.ofSeconds(1)),
                Arguments.of(10,20,-1,Duration.ofSeconds(120),Duration.ofSeconds(1)),
                Arguments.of(10,20,2,Duration.ofSeconds(120),Duration.ofSeconds(1)),
                Arguments.of(10,20,1,Duration.ZERO,Duration.ofSeconds(1)),
                Arguments.of(10,20,1,Duration.ofSeconds(121),Duration.ofSeconds(1)),
                Arguments.of(10,20,1,null,Duration.ofSeconds(1)),
                Arguments.of(10,20,1,Duration.ofSeconds(120),Duration.ofSeconds(-1)),
                Arguments.of(10,20,1,Duration.ofSeconds(120),null));
    }
}
