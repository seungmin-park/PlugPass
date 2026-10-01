package com.plugpass.ingestion;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PublicDataProperties.class)
public class PublicDataConfiguration {
    @Bean(destroyMethod = "close")
    PublicDataClient publicDataClient(PublicDataProperties properties) {
        return new DefaultPublicDataClient(properties, Clock.systemUTC());
    }
}
