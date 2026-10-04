package reliability;

import com.plugpass.PlugPassApplication;
import com.plugpass.ingestion.domain.SyncStatus;
import com.plugpass.ingestion.service.StationSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Test classpath only; each launch collects once through the real HTTP/DB boundary. */
public final class RestartProbeApplication {
    public static void main(String[] arguments) {
        new SpringApplicationBuilder(PlugPassApplication.class, CollectionBoundary.class).run(arguments);
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class CollectionBoundary {
        @Bean
        ApplicationRunner collectProbeData(StationSyncService stationSyncService,
                @Value("${restart-probe.collect:false}") boolean collect) {
            return arguments -> {
                if (collect && stationSyncService.synchronize().status() != SyncStatus.SUCCESS) {
                    throw new IllegalStateException("Restart probe collection failed");
                }
                System.out.println("RESTART_PROBE_READY");
            };
        }
    }
}
