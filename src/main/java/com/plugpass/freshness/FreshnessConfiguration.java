package com.plugpass.freshness;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FreshnessConfiguration {
    @Bean
    FreshnessPolicy freshnessPolicy(@Value("${plugpass.freshness.max-age:PT10M}") String maxAge) {
        return new FreshnessPolicy(Duration.parse(maxAge));
    }
}
