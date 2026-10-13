package com.plugpass.ingestion.domain;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CollectionScheduleTests {
    private final Instant now = Instant.parse("2026-10-13T00:00:00Z");
    @Test
    @DisplayName("최초 수집은 전체 기본 정보를 요청한다")
    void selectsInitialCatalog() {
        assertThat(CollectionSchedule.select(null, now, now)).isEqualTo(SyncKind.CATALOG);
    }
    @Test
    @DisplayName("최근 수집이 10분 이내이고 기본 정보가 하루 이내이면 상태 변경분을 요청한다")
    void selectsRecentStatus() {
        assertThat(CollectionSchedule.select(now.minusSeconds(86399), now.minusSeconds(599), now)).isEqualTo(SyncKind.STATUS);
    }
    @Test
    @DisplayName("기본 정보 수집이 하루가 되면 전체 정보를 갱신한다")
    void refreshesDailyCatalog() {
        assertThat(CollectionSchedule.select(now.minusSeconds(86400), now, now)).isEqualTo(SyncKind.CATALOG);
    }
    @Test
    @DisplayName("마지막 성공 이후 10분이 되면 변경분 누락 복구를 위해 전체 정보를 갱신한다")
    void recoversMissedWindow() {
        assertThat(CollectionSchedule.select(now.minusSeconds(3600), now.minusSeconds(600), now)).isEqualTo(SyncKind.CATALOG);
    }
}
