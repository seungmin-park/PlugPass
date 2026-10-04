package com.plugpass.ingestion.domain;

import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncRunTests {
    @Test
    @DisplayName("실행 시작 시각이 없으면 생성하지 않는다")
    void rejectsMissingStart() {
        assertThatThrownBy(() -> SyncRun.builder().startedAt(null).build()).isInstanceOf(IllegalArgumentException.class).hasMessage("startedAt must not be null");
    }
    @Test
    @DisplayName("이미 끝난 실행을 다시 끝내려 하면 최초 결과를 유지한다")
    void rejectsRepeatedCompletion() {
        Instant startedAt = Instant.parse("2026-10-02T00:00:00Z");
        SyncRun run = SyncRun.builder().startedAt(startedAt).build();
        run.complete(2, null, null, startedAt.plusSeconds(1));
        assertThatThrownBy(() -> run.complete(0, 1, "TIMEOUT", startedAt.plusSeconds(2))).isInstanceOf(IllegalStateException.class).hasMessage("sync run is already completed");
        assertThat(run.getStatus()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(run.getProcessedCount()).isEqualTo(2);
        assertThat(run.getCompletedAt()).isEqualTo(startedAt.plusSeconds(1));
    }
    @Test
    @DisplayName("실패한 페이지 수를 명시적으로 기록하고 결과에도 전달한다")
    void recordsFailurePageCount() {
        Instant startedAt = Instant.parse("2026-10-02T00:00:00Z");
        SyncRun run = SyncRun.builder().startedAt(startedAt).build();
        run.complete(0,1,"TIMEOUT",startedAt.plusSeconds(1));
        assertThat(run.getFailedPageCount()).isEqualTo(1);
        assertThat(run.result().failedPageCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @DisplayName("잘못된 완료 입력은 실행의 상태와 시각을 바꾸지 않는다")
    @MethodSource("invalidCompletions")
    void rejectsInvalidCompletion(long count, Integer failedPage, String code, Instant completedAt) {
        Instant startedAt = Instant.parse("2026-10-02T00:00:00Z");
        SyncRun run = SyncRun.builder().startedAt(startedAt).build();
        assertThatThrownBy(() -> run.complete(count, failedPage, code, completedAt)).isInstanceOf(IllegalArgumentException.class);
        assertThat(run.getStatus()).isEqualTo(SyncStatus.RUNNING);
        assertThat(run.getStartedAt()).isEqualTo(startedAt);
        assertThat(run.getCompletedAt()).isNull();
        assertThat(run.getProcessedCount()).isZero();
        assertThat(run.getFailedPage()).isNull();
        assertThat(run.getFailureCode()).isNull();
    }
    static Stream<Arguments> invalidCompletions() {
        Instant startedAt = Instant.parse("2026-10-02T00:00:00Z");
        return Stream.of(Arguments.of(-1L,null,null,startedAt), Arguments.of(0L,null,null,null),
                Arguments.of(0L,null,null,startedAt.minusSeconds(1)), Arguments.of(0L,0,"TIMEOUT",startedAt),
                Arguments.of(0L,1,null,startedAt), Arguments.of(0L,null,"TIMEOUT",startedAt), Arguments.of(0L,1," ",startedAt));
    }
}
