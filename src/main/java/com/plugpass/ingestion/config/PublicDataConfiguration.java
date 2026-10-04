package com.plugpass.ingestion.config;

import com.plugpass.ingestion.client.DefaultPublicDataClient;
import com.plugpass.ingestion.client.PublicDataClient;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PublicDataProperties.class)
public class PublicDataConfiguration {
    @Bean
    Clock applicationClock() { return Clock.systemUTC(); }

    @Bean(destroyMethod = "close")
    PublicDataClient publicDataClient(PublicDataProperties properties, Clock clock) {
        return new DefaultPublicDataClient(properties, clock);
    }
}
