package mysql;

import com.plugpass.PlugPassApplication;
import com.plugpass.ingestion.domain.SyncRun;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.service.StationSyncService;
import com.plugpass.ingestion.dto.SyncResult;
import java.sql.ResultSet;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.ingestion.dto.StationSnapshot;
import com.plugpass.ingestion.service.StationUpsertService;
import com.plugpass.station.domain.Charger;
import com.plugpass.station.domain.ChargerDetails;
import com.plugpass.station.domain.ChargerId;
import com.plugpass.station.domain.ChargerStatus;
import com.plugpass.station.domain.GeoPoint;
import com.plugpass.station.domain.Station;
import com.plugpass.station.repository.ChargerRepository;
import com.plugpass.station.repository.StationRepository;
import java.time.Instant;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MySqlDeploymentTests {
    private ConfigurableApplicationContext context;

    @AfterEach
    void deleteCreatedDataAndCloseApplication() {
        if (context == null) {
            return;
        }
        try {
            context.getBean(ChargerRepository.class).deleteAllInBatch();
            context.getBean(StationRepository.class).deleteAllInBatch();
            context.getBean(SyncRunRepository.class).deleteAllInBatch();
        } finally {
            context.close();
        }
    }

    @Test
    @DisplayName("배포 프로필은 MySQL 스키마를 버전 관리하고 앱 재시작 후 데이터와 수집 이력을 유지한다")
    void migratesAndPreservesDataAcrossApplicationRestart() {
        startApplication();
        Instant collectedAt = Instant.parse("2026-10-12T00:00:00.123456Z");
        ChargerId chargerId = new ChargerId("ME", "028260005", "02");
        ChargerDetails details = new ChargerDetails("03", "24시간", "N", null, "지하 🔌", null, null, null, null);
        StationSnapshot snapshot = new StationSnapshot(chargerId, "인천 충전소 🔋", new GeoPoint(37.5, 126.6),
                ChargerStatus.AVAILABLE, "2", details, null, collectedAt);
        context.getBean(StationUpsertService.class).upsert(snapshot);
        SyncRun syncRun = SyncRun.builder().startedAt(collectedAt).build();
        syncRun.complete(1, null, null, collectedAt.plusSeconds(1));
        context.getBean(SyncRunRepository.class).save(syncRun);
        context.close();
        context = null;

        startApplication();

        Charger restored = context.getBean(ChargerRepository.class).findByIdentity(chargerId).orElseThrow();
        assertThat(restored.getId()).isEqualTo(chargerId);
        assertThat(restored.getStatus()).isEqualTo(ChargerStatus.AVAILABLE);
        assertThat(restored.getDetails()).isEqualTo(details);
        assertThat(restored.getCollectedAt()).isEqualTo(collectedAt);
        assertThat(restored.getCreatedAt()).isEqualTo(collectedAt);
        assertThat(restored.getSourceObservedAt()).isNull();
        assertThat(context.getBean(StationRepository.class).findByProviderAndStationId("ME", "028260005")
                .orElseThrow().getName()).isEqualTo("인천 충전소 🔋");
        assertThat(context.getBean(SyncRunRepository.class).findAll()).singleElement()
                .satisfies(restoredRun -> {
                    assertThat(restoredRun.getStartedAt()).isEqualTo(collectedAt);
                    assertThat(restoredRun.getProcessedCount()).isEqualTo(1);
                });
        JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
        assertThat(jdbcTemplate.queryForObject("select version()", String.class)).startsWith("8.4.");
        assertThat(jdbcTemplate.queryForList("select version from flyway_schema_history where success = 1", String.class))
                .containsExactly("1");
    }

    @Test
    @DisplayName("MySQL에서도 식별자의 대소문자·끝 공백·선행 0을 구분하고 재수집은 같은 행을 갱신한다")
    void preservesExactCompositeIdentitiesAndUpserts() {
        startApplication();
        Instant collectedAt = Instant.parse("2026-10-12T00:00:00Z");
        List<StationSnapshot> snapshots = List.of(
                new StationSnapshot(new ChargerId("ME", "AbC", "02"), "첫 충전소", new GeoPoint(37.5, 126.6), ChargerStatus.AVAILABLE, "2", null, null, collectedAt),
                new StationSnapshot(new ChargerId("ME", "abc", "02"), "소문자 ID", new GeoPoint(37.5, 126.6), ChargerStatus.AVAILABLE, "2", null, null, collectedAt),
                new StationSnapshot(new ChargerId("ME", "AbC", "02 "), "첫 충전소", new GeoPoint(37.5, 126.6), ChargerStatus.AVAILABLE, "2", null, null, collectedAt),
                new StationSnapshot(new ChargerId("me", "AbC", "02"), "다른 기관", new GeoPoint(37.5, 126.6), ChargerStatus.AVAILABLE, "2", null, null, collectedAt),
                new StationSnapshot(new ChargerId("ME", "AbC ", "02"), "공백 ID", new GeoPoint(37.5, 126.6), ChargerStatus.AVAILABLE, "2", null, null, collectedAt));
        StationUpsertService stationUpsertService = context.getBean(StationUpsertService.class);
        stationUpsertService.upsertPage(snapshots);

        stationUpsertService.upsert(new StationSnapshot(new ChargerId("ME", "AbC", "02"), "갱신 이름",
                new GeoPoint(37.5, 126.6), ChargerStatus.OCCUPIED, "3", null, null, collectedAt.plusSeconds(60)));

        ChargerRepository chargerRepository = context.getBean(ChargerRepository.class);
        assertThat(chargerRepository.findAll()).extracting(Charger::getId).containsExactlyInAnyOrder(
                new ChargerId("ME", "AbC", "02"), new ChargerId("ME", "abc", "02"),
                new ChargerId("ME", "AbC", "02 "), new ChargerId("me", "AbC", "02"), new ChargerId("ME", "AbC ", "02"));
        assertThat(context.getBean(StationRepository.class).count()).isEqualTo(4);
        Charger updated = chargerRepository.findByIdentity(new ChargerId("ME", "AbC", "02")).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ChargerStatus.OCCUPIED);
        assertThat(updated.getCreatedAt()).isEqualTo(collectedAt);
        assertThat(updated.getUpdatedAt()).isEqualTo(collectedAt.plusSeconds(60));
        assertThat(chargerRepository.findByIdentity(new ChargerId("ME", "AbC", "02 ")).orElseThrow().getStatus())
                .isEqualTo(ChargerStatus.AVAILABLE);
    }

    @Test
    @DisplayName("MySQL 충전기 저장 실패는 같은 서비스 트랜잭션의 충전소 생성도 롤백한다")
    void rollsBackStationWhenChargerInsertFails() {
        startApplication();
        JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
        assertThat(jdbcTemplate.queryForObject("select @@global.sql_mode", String.class)).doesNotContain("STRICT");
        assertThat(jdbcTemplate.queryForObject("select @@session.sql_mode", String.class)).contains("STRICT_TRANS_TABLES");
        StationSnapshot snapshot = new StationSnapshot(new ChargerId("ME", "rollback", "01"), "롤백 충전소",
                new GeoPoint(37.5, 126.6), ChargerStatus.UNKNOWN, "x".repeat(256), null, null,
                Instant.parse("2026-10-12T00:00:00Z"));

        assertThatThrownBy(() -> context.getBean(StationUpsertService.class).upsert(snapshot))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(context.getBean(ChargerRepository.class).count()).isZero();
        assertThat(context.getBean(StationRepository.class).count()).isZero();
    }

    @Test
    @DisplayName("MySQL 충전소 unique 제약은 같은 기관·충전소 ID의 중복 저장을 거부한다")
    void rejectsDuplicateStationIdentity() {
        startApplication();
        Instant collectedAt = Instant.parse("2026-10-12T00:00:00Z");
        ChargerId chargerId = new ChargerId("ME", "unique", "01");
        context.getBean(StationUpsertService.class).upsert(new StationSnapshot(chargerId, "충전소",
                new GeoPoint(37.5, 126.6), ChargerStatus.AVAILABLE, "2", null, null, collectedAt));
        StationRepository stationRepository = context.getBean(StationRepository.class);
        Station duplicate = Station.builder().provider("ME").stationId("unique").name("중복")
                .location(new GeoPoint(37.5, 126.6)).createdAt(collectedAt).build();

        assertThatThrownBy(() -> stationRepository.save(duplicate)).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(stationRepository.count()).isEqualTo(1);
        assertThat(context.getBean(ChargerRepository.class).findAll()).extracting(Charger::getId).containsExactly(chargerId);
    }

    @Test
    @DisplayName("MySQL 충전기 unique 제약은 같은 복합 식별자의 중복 저장을 거부한다")
    void rejectsDuplicateChargerIdentity() {
        startApplication();
        Instant collectedAt = Instant.parse("2026-10-12T00:00:00Z");
        ChargerId chargerId = new ChargerId("ME", "charger-unique", "01");
        context.getBean(StationUpsertService.class).upsert(new StationSnapshot(chargerId, "충전소",
                new GeoPoint(37.5, 126.6), ChargerStatus.AVAILABLE, "2", null, null, collectedAt));
        Station station = context.getBean(StationRepository.class)
                .findByProviderAndStationId("ME", "charger-unique").orElseThrow();
        Charger duplicate = Charger.builder().id(chargerId).station(station).status(ChargerStatus.AVAILABLE)
                .rawStatus("2").collectedAt(collectedAt).build();
        ChargerRepository chargerRepository = context.getBean(ChargerRepository.class);

        assertThatThrownBy(() -> chargerRepository.save(duplicate)).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(chargerRepository.findAll()).extracting(Charger::getId).containsExactly(chargerId);
        assertThat(context.getBean(StationRepository.class).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("MySQL FK 제약은 충전기가 남아 있는 부모 충전소 삭제를 거부한다")
    void rejectsDeletionOfStationWithCharger() {
        startApplication();
        ChargerId chargerId = new ChargerId("ME", "foreign-key", "01");
        context.getBean(StationUpsertService.class).upsert(new StationSnapshot(chargerId, "충전소",
                new GeoPoint(37.5, 126.6), ChargerStatus.AVAILABLE, "2", null, null,
                Instant.parse("2026-10-12T00:00:00Z")));
        StationRepository stationRepository = context.getBean(StationRepository.class);

        assertThatThrownBy(stationRepository::deleteAllInBatch).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(stationRepository.count()).isEqualTo(1);
        assertThat(context.getBean(ChargerRepository.class).findAll()).extracting(Charger::getId).containsExactly(chargerId);
    }

    @Test
    @DisplayName("관리 스키마가 엔티티와 다르면 배포 기동을 거부하고 자동으로 고치지 않는다")
    void rejectsSchemaDriftWithoutModifyingIt() throws Exception {
        startApplication();
        context.getBean(JdbcTemplate.class).execute("alter table station rename column name to removed_name");
        context.close();
        context = null;
        try {
            assertThatThrownBy(() -> new SpringApplicationBuilder(PlugPassApplication.class, MySqlPersistenceConfiguration.class)
                    .profiles("deployment").web(WebApplicationType.NONE)
                    .run("--logging.level.root=ERROR", "--spring.main.banner-mode=off"))
                    .hasStackTraceContaining("Schema validation: missing column [name]");
            try (Connection connection = databaseConnection(); Statement statement = connection.createStatement()) {
                assertThat(statement.executeQuery("select removed_name from station").next()).isFalse();
            }
        } finally {
            try (Connection connection = databaseConnection(); Statement statement = connection.createStatement()) {
                statement.execute("alter table station rename column removed_name to name");
            }
        }
    }

    @Test
    @DisplayName("다른 DB 연결이 수집 잠금을 소유하면 외부 요청과 실행 이력을 만들지 않는다")
    void skipsWhenAnotherConnectionOwnsIngestion() throws Exception {
        startApplication();
        try (Connection connection = databaseConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery("select GET_LOCK('plugpass.ingestion', 0)")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
            SyncResult skipped = context.getBean(StationSyncService.class).synchronize();
            assertThat(skipped.status()).isEqualTo(SyncStatus.SKIPPED);
            assertThat(skipped.requestCount()).isZero();
            assertThat(context.getBean(SyncRunRepository.class).count()).isZero();
            try (ResultSet result = statement.executeQuery("select RELEASE_LOCK('plugpass.ingestion')")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
        }
        SyncResult resumed = context.getBean(StationSyncService.class).synchronize();
        assertThat(resumed.status()).isEqualTo(SyncStatus.FAILURE);
        assertThat(resumed.failureCode()).isEqualTo("AUTHENTICATION");
        assertThat(context.getBean(SyncRunRepository.class).count()).isEqualTo(1);
    }

    private Connection databaseConnection() throws Exception {
        return DriverManager.getConnection(System.getenv("PLUGPASS_DATABASE_URL"),
                System.getenv("PLUGPASS_DATABASE_USERNAME"), System.getenv("PLUGPASS_DATABASE_PASSWORD"));
    }

    private void startApplication() {
        assertThatCode(() -> context = new SpringApplicationBuilder(PlugPassApplication.class, MySqlPersistenceConfiguration.class)
                .profiles("deployment").web(WebApplicationType.NONE)
                .run("--logging.level.root=ERROR", "--spring.main.banner-mode=off"))
                .as("배포 설정으로 실제 MySQL에 연결하고 관리 스키마로 기동해야 한다")
                .doesNotThrowAnyException();
    }
}
