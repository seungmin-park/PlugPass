package com.plugpass.places.domain;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceSearchBudgetTests {
    @Test
    @DisplayName("같은 1분 안에서는 한도만큼만 주소 검색을 허용한다")
    void allowsSearchesUpToLimitWithinMinute() {
        PlaceSearchBudget budget = new PlaceSearchBudget(2);
        Instant start = Instant.parse("2026-11-04T00:00:00Z");

        assertThat(budget.tryAcquire(start)).isTrue();
        assertThat(budget.tryAcquire(start.plusSeconds(30))).isTrue();
        assertThat(budget.tryAcquire(start.plusSeconds(59))).isFalse();
    }

    @Test
    @DisplayName("첫 검색 후 정확히 1분이 지나면 새 한도로 다시 허용한다")
    void resetsAtMinuteBoundary() {
        PlaceSearchBudget budget = new PlaceSearchBudget(1);
        Instant start = Instant.parse("2026-11-04T00:00:00Z");
        budget.tryAcquire(start);

        assertThat(budget.tryAcquire(start.plusSeconds(59))).isFalse();
        assertThat(budget.tryAcquire(start.plusSeconds(60))).isTrue();
        assertThat(budget.tryAcquire(start.plusSeconds(61))).isFalse();
    }

    @ParameterizedTest
    @DisplayName("분당 한도는 1 이상이어야 한다")
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveLimit(int searchesPerMinute) {
        assertThatThrownBy(() -> new PlaceSearchBudget(searchesPerMinute))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("searches per minute must be positive");
    }
}
