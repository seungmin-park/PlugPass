package com.plugpass.ingestion;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.FixedDelayTask;
import com.plugpass.station.ChargerRepository;
import com.plugpass.station.StationRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SyncSchedulerTests.ExternalBoundary.class)
class SyncSchedulerTests {
    @Autowired private StationSyncService stationSyncService;
    @Autowired private SyncRunRepository syncRunRepository;
    @Autowired private ChargerRepository chargerRepository;
    @Autowired private StationRepository stationRepository;
    @Autowired private ControlledClient controlledClient;
    @Autowired private PublicDataProperties publicDataProperties;
    @AfterEach
    void cleanOwnedState() {
        controlledClient.release.countDown(); controlledClient.reset();
        chargerRepository.deleteAllInBatch(); stationRepository.deleteAllInBatch(); syncRunRepository.deleteAllInBatch();
    }
    @Test
    @DisplayName("수집 중 스케줄·직접 호출이 겹치면 기다리지 않고 건너뛴다")
    void skipsOverlappingExecution() throws Exception {
        SyncScheduler scheduler = new SyncScheduler(stationSyncService);
        controlledClient.started = new CountDownLatch(1); controlledClient.release = new CountDownLatch(1);
        CompletableFuture<Void> running = CompletableFuture.runAsync(scheduler::tick);
        try {
            assertThat(controlledClient.started.await(3,TimeUnit.SECONDS)).isTrue();
            CompletableFuture<SyncResult> duplicate = CompletableFuture.supplyAsync(stationSyncService::synchronize);
            assertThatCode(() -> duplicate.get(500,TimeUnit.MILLISECONDS)).as("중복 호출은 실행 완료를 기다리면 안 된다").doesNotThrowAnyException();
            assertThat(duplicate.get().status().name()).isEqualTo("SKIPPED");
            assertThat(controlledClient.requests).hasValue(1);
        } finally {
            controlledClient.release.countDown(); running.get(3,TimeUnit.SECONDS);
        }
        assertThat(syncRunRepository.findAll()).extracting(SyncRun::getStatus).containsExactly(SyncStatus.SUCCESS);
    }
    @Test
    @DisplayName("예상하지 못한 예외도 실패 이력을 남기고 다음 실행 소유권을 풀어 준다")
    void releasesOwnershipAfterException() {
        controlledClient.failure = new IllegalStateException("fixture failure");
        assertThatThrownBy(stationSyncService::synchronize).isInstanceOf(IllegalStateException.class);
        assertThat(syncRunRepository.findAll()).extracting(SyncRun::getStatus).containsExactly(SyncStatus.FAILURE);
        assertThat(syncRunRepository.findAll()).extracting(SyncRun::getFailureCode).containsExactly("INTERNAL");
        controlledClient.failure = null;
        new SyncScheduler(stationSyncService).tick();
        assertThat(syncRunRepository.findAll()).extracting(SyncRun::getStatus).containsExactlyInAnyOrder(SyncStatus.FAILURE,SyncStatus.SUCCESS);
        assertThat(controlledClient.requests).hasValue(2);
    }
    @Test
    @DisplayName("기본 설정은 반복 수집을 등록하지 않는다")
    void disablesByDefault() {
        contextRunner("").run(context -> assertThat(context).doesNotHaveBean(SyncScheduler.class));
    }
    @Test
    @DisplayName("반복 수집 활성화에 인증키가 없으면 기동을 거부한다")
    void rejectsEnabledWithoutAuthentication() {
        contextRunner("").withPropertyValues("plugpass.ingestion.schedule.enabled=true","plugpass.ingestion.schedule.delay=PT30M")
                .run(context -> assertThat(context).hasFailed());
    }
    @Test
    @DisplayName("호출 예산에 비해 너무 짧은 간격은 거부한다")
    void rejectsUnsafeDelay() {
        assertThatThrownBy(() -> new SyncScheduleProperties(true,Duration.ofMinutes(29)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("schedule delay must be at least PT30M");
    }
    @Test
    @DisplayName("명시적 활성화와 인증키가 있으면 고정 지연 작업을 등록한다")
    void registersFixedDelayWithAuthentication() {
        contextRunner("fixture-only").withPropertyValues("plugpass.ingestion.schedule.enabled=true","plugpass.ingestion.schedule.delay=PT30M")
                .run(context -> {
                    assertThat(context).hasSingleBean(SyncScheduler.class);
                    ScheduledAnnotationBeanPostProcessor processor = context.getBean(ScheduledAnnotationBeanPostProcessor.class);
                    assertThat(processor.getScheduledTasks()).hasSize(1);
                    assertThat(processor.getScheduledTasks().iterator().next().getTask()).isInstanceOf(FixedDelayTask.class);
                });
    }
    private ApplicationContextRunner contextRunner(String serviceKey) {
        PublicDataProperties missingKey = new PublicDataProperties(publicDataProperties.endpoint(),serviceKey,9999,"28",Duration.ofSeconds(3),Duration.ofSeconds(10));
        return new ApplicationContextRunner().withUserConfiguration(SyncSchedulingConfiguration.class)
                .withBean(StationSyncService.class,() -> stationSyncService).withBean(PublicDataProperties.class,() -> missingKey);
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class ExternalBoundary {
        @Bean @Primary ControlledClient controlledClient() { return new ControlledClient(); }
    }
    static class ControlledClient implements PublicDataClient {
        final AtomicInteger requests = new AtomicInteger();
        volatile CountDownLatch started = new CountDownLatch(0);
        volatile CountDownLatch release = new CountDownLatch(0);
        volatile RuntimeException failure;
        public StationPage fetchPage(int page) {
            requests.incrementAndGet(); started.countDown();
            try { if (!release.await(3,TimeUnit.SECONDS)) { throw new IllegalStateException("fixture latch timed out"); } }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("fixture interrupted"); }
            if (failure != null) { throw failure; }
            return new StationPage(1,10,0,List.of());
        }
        void reset() { requests.set(0); started = new CountDownLatch(0); release = new CountDownLatch(0); failure = null; }
        public StationPage fetchPage(int page, Duration remaining) { return fetchPage(page); }
        public void close() { }
    }
}
