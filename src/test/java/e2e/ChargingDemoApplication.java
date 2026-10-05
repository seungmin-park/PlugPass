package e2e;

import com.plugpass.PlugPassApplication;
import com.plugpass.ingestion.client.DefaultPublicDataClient;
import com.plugpass.ingestion.client.PublicDataClient;
import com.plugpass.ingestion.config.PublicDataProperties;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.service.StationSyncService;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

public final class ChargingDemoApplication {
    public static void main(String[] arguments) {
        new SpringApplicationBuilder(PlugPassApplication.class, DemoConfiguration.class).run(arguments);
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class DemoConfiguration {
        @Bean @Primary
        Clock demoClock() { return Clock.fixed(Instant.parse("2026-10-05T00:00:00Z"), ZoneOffset.UTC); }

        @Bean(destroyMethod = "close")
        DemoProvider demoProvider() throws IOException { return new DemoProvider(); }

        @Bean(destroyMethod = "close") @Primary
        PublicDataClient demoPublicDataClient(DemoProvider provider, Clock clock) {
            PublicDataProperties properties = new PublicDataProperties(provider.endpoint(), "synthetic-browser-demo", 10,
                    "11", Duration.ofSeconds(2), Duration.ofSeconds(2));
            return new DefaultPublicDataClient(properties, clock);
        }

        @Bean
        ApplicationRunner collectDemoData(StationSyncService stationSyncService) {
            return arguments -> {
                if (stationSyncService.synchronize().status() != SyncStatus.SUCCESS) {
                    throw new IllegalStateException("Browser demo collection failed");
                }
                System.out.println("CHARGING_DEMO_READY");
            };
        }
    }
}
