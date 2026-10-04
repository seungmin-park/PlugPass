package performance;

import com.plugpass.PlugPassApplication;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.station.repository.StationRepository;
import com.plugpass.station.repository.ChargerRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.hibernate.Version;
import org.hibernate.stat.Statistics;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Standalone test-classpath server: production HTTP routes plus measurement metadata. */
public final class PerformanceApplication {
    public static void main(String[] arguments) {
        new SpringApplicationBuilder(PlugPassApplication.class, MeasurementBoundary.class)
                .properties("spring.jpa.properties.hibernate.generate_statistics=true",
                        "logging.level.org.hibernate.stat=OFF", "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF",
                        "plugpass.ingestion.schedule.enabled=false")
                .run(arguments);
    }
    @TestConfiguration(proxyBeanMethods = false)
    public static class MeasurementBoundary {
        @Bean @Primary Clock measurementClock() {
            return Clock.fixed(Instant.parse(PerformanceDataset.OBSERVED_AT), ZoneOffset.UTC);
        }
        @Bean ApplicationRunner seedMeasurementData(StationRepository stationRepository, ChargerRepository chargerRepository, SyncRunRepository syncRunRepository) {
            return arguments -> {
                new PerformanceDataset(stationRepository, chargerRepository, syncRunRepository).seed(10000);
                System.out.println("PERFORMANCE_DATA_READY grid-v1 seed=0 stations=10000 chargers=50000");
            };
        }
        @Bean MeasurementController measurementController(DataSource dataSource, EntityManagerFactory entityManagerFactory) {
            return new MeasurementController(dataSource, entityManagerFactory.unwrap(SessionFactory.class).getStatistics());
        }
    }
    @RestController
    public static class MeasurementController {
        private final JdbcTemplate jdbcTemplate;
        private final Statistics statistics;
        MeasurementController(DataSource dataSource, Statistics statistics) {
            this.jdbcTemplate = new JdbcTemplate(dataSource);
            this.statistics = statistics;
        }
        @GetMapping("/__performance")
        public Map<String, Object> metadata() {
            return Map.of("stations", jdbcTemplate.queryForObject("select count(*) from station", Long.class),
                    "chargers", jdbcTemplate.queryForObject("select count(*) from charger", Long.class),
                    "statements", statistics.getPrepareStatementCount(), "entitiesLoaded", statistics.getEntityLoadCount(),
                    "java", System.getProperty("java.version"), "boot", SpringBootVersion.getVersion(),
                    "hibernate", Version.getVersionString(), "statisticsEnabled", statistics.isStatisticsEnabled());
        }
    }
}
