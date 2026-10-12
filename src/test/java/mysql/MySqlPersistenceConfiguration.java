package mysql;

import com.plugpass.ingestion.domain.SyncRun;
import com.plugpass.ingestion.repository.SyncRunRepository;
import com.plugpass.station.domain.Station;
import com.plugpass.station.repository.StationRepository;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Match the shipped application's entities, excluding the old test-only PersistenceProbe. */
@TestConfiguration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = {Station.class, SyncRun.class})
@EnableJpaRepositories(basePackageClasses = {StationRepository.class, SyncRunRepository.class})
public class MySqlPersistenceConfiguration {
}
