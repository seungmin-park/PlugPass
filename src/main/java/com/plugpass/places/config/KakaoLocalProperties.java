package com.plugpass.places.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("plugpass.kakao-local")
public record KakaoLocalProperties(URI baseUrl, String restApiKey, Duration timeout, Integer maxSearchesPerMinute) {
    /** 30 searches x 2 upstream calls x 1440 minutes stays under Kakao Local's free daily quota. */
    private static final int DEFAULT_SEARCHES_PER_MINUTE = 30;
    public KakaoLocalProperties {
        if (baseUrl == null) baseUrl = URI.create("https://dapi.kakao.com");
        if (restApiKey == null) restApiKey = "";
        if (timeout == null) timeout = Duration.ofSeconds(3);
        if (maxSearchesPerMinute == null) maxSearchesPerMinute = DEFAULT_SEARCHES_PER_MINUTE;
        if (maxSearchesPerMinute < 1) throw new IllegalArgumentException("place searches per minute must be positive");
        if (timeout.isZero() || timeout.isNegative() || timeout.compareTo(Duration.ofSeconds(4)) > 0)
            throw new IllegalArgumentException("place request timeout must be positive and at most 4 seconds");
    }
    @Override public String toString() { return "KakaoLocalProperties[credentials redacted]"; }
}
