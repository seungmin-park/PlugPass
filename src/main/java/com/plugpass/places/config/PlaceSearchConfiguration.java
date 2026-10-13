package com.plugpass.places.config;

import com.plugpass.places.client.KakaoPlaceSearchClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KakaoLocalProperties.class)
public class PlaceSearchConfiguration {
    @Bean(destroyMethod = "close")
    KakaoPlaceSearchClient placeSearchClient(KakaoLocalProperties properties, JsonMapper mapper) {
        return new KakaoPlaceSearchClient(properties, mapper);
    }
}
