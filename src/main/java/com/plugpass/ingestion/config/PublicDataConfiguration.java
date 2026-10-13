package com.plugpass.ingestion.config;

import com.plugpass.ingestion.client.DefaultPublicDataClient;

import java.time.Clock;
import javax.sql.DataSource;
import com.plugpass.ingestion.client.IngestionLock;
import com.plugpass.ingestion.client.DatabaseIngestionLock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({PublicDataProperties.class, IngestionProperties.class})
public class PublicDataConfiguration {
    @Bean
    IngestionLock ingestionLock(DataSource dataSource) { return new DatabaseIngestionLock(dataSource); }

    @Bean
    Clock applicationClock() { return Clock.systemUTC(); }

    @Bean(destroyMethod = "close")
    DefaultPublicDataClient publicDataClient(PublicDataProperties properties, Clock clock) {
        return new DefaultPublicDataClient(properties, clock);
    }
}
