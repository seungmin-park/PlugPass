package com.plugpass.ingestion.command;

import com.plugpass.PlugPassApplication;
import com.plugpass.ingestion.domain.SyncRun;
import com.plugpass.ingestion.domain.SyncKind;
import com.plugpass.ingestion.client.PublicStatusClient;
import com.plugpass.ingestion.dto.StatusPage;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.ingestion.client.PublicDataClient;
import com.plugpass.ingestion.dto.StationPage;
import java.time.Duration;
import com.plugpass.ingestion.service.ScheduledSyncService;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;

class IngestionCommandTests {
    @Test
    @DisplayName("일회 수집이 모든 페이지를 완료하면 성공 종료 코드와 성공 이력을 남긴다")
    void reportsCompletedCollection() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(PlugPassApplication.class, ExternalBoundary.class)
                .web(WebApplicationType.NONE).run("--plugpass.ingestion.command.enabled=true", "--logging.level.root=ERROR")) {
            assertThat(context.getBean(ExitCodeGenerator.class).getExitCode()).isZero();
            assertThat(context.getBean(SyncRunRepository.class).findAll()).extracting(SyncRun::getStatus)
                    .containsExactly(SyncStatus.SUCCESS);
        }
    }

    @Test
    @DisplayName("기본 기동에는 일회 수집을 실행하거나 이력을 만들지 않는다")
    void doesNotCollectByDefault() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(PlugPassApplication.class)
                .web(WebApplicationType.NONE).run("--logging.level.root=ERROR")) {
            assertThat(context.getBeansOfType(ApplicationRunner.class)).doesNotContainKey("ingestionCommand");
            assertThat(context.getBean(SyncRunRepository.class).count()).isZero();
        }
    }

    @Test
    @DisplayName("명시적 일회 수집은 기동 즉시 실행하고 인증 실패를 종료 코드로 전달한다")
    void runsOnceAndReportsFailure() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(PlugPassApplication.class)
                .web(WebApplicationType.NONE).run("--plugpass.ingestion.command.enabled=true",
                        "--plugpass.public-data.service-key=", "--logging.level.root=ERROR")) {
            assertThat(context.getBeansOfType(ApplicationRunner.class)).containsKey("ingestionCommand");
            assertThat(context.getBean(ExitCodeGenerator.class).getExitCode()).isEqualTo(1);
            assertThat(context.getBean(SyncRunRepository.class).findAll()).extracting(SyncRun::getStatus)
                    .containsExactly(SyncStatus.FAILURE);
        }
    }

    @Test
    @DisplayName("상태 모드는 기본 정보를 수집하지 않고 상태 실행 이력만 남긴다")
    void runsStatusMode() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(PlugPassApplication.class, ExternalBoundary.class)
                .web(WebApplicationType.NONE).run("--plugpass.ingestion.command.enabled=true",
                        "--plugpass.ingestion.command.mode=status", "--logging.level.root=ERROR")) {
            assertThat(context.getBean(ExitCodeGenerator.class).getExitCode()).isZero();
            assertThat(context.getBean(SyncRunRepository.class).findAll()).extracting(SyncRun::getKind)
                    .containsExactly(SyncKind.STATUS);
        }
    }

    @Test
    @DisplayName("자동 실행은 최초 전체 수집 이후 다음 호출에서 상태 수집을 선택한다")
    void switchesFromInitialCatalogToStatus() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(PlugPassApplication.class, ExternalBoundary.class)
                .web(WebApplicationType.NONE).run("--plugpass.ingestion.command.enabled=true",
                        "--plugpass.ingestion.command.mode=auto", "--logging.level.root=ERROR")) {
            assertThat(context.getBean(SyncRunRepository.class).findAll()).extracting(SyncRun::getKind)
                    .containsExactly(SyncKind.CATALOG);
            assertThat(context.getBean(ScheduledSyncService.class).synchronize().status()).isEqualTo(SyncStatus.SUCCESS);
            assertThat(context.getBean(SyncRunRepository.class).findAll()).extracting(SyncRun::getKind)
                    .containsExactlyInAnyOrder(SyncKind.CATALOG, SyncKind.STATUS);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ExternalBoundary {
        @Bean @Primary PublicStatusClient emptyStatusProvider() {
            return (page, remaining) -> new StatusPage(page, 10, 0, List.of());
        }
        @Bean @Primary PublicDataClient emptyProvider() {
            return new PublicDataClient() {
                public StationPage fetchPage(int page) { return new StationPage(page, 10, 0, List.of()); }
                public StationPage fetchPage(int page, Duration remaining) { return fetchPage(page); }
                public void close() { }
            };
        }
    }
}
