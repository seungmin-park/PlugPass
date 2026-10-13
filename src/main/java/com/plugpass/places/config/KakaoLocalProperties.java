package com.plugpass.places.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("plugpass.kakao-local")
public record KakaoLocalProperties(URI baseUrl, String restApiKey, Duration timeout) {
    public KakaoLocalProperties {
        if (baseUrl == null) baseUrl = URI.create("https://dapi.kakao.com");
        if (restApiKey == null) restApiKey = "";
        if (timeout == null) timeout = Duration.ofSeconds(3);
        if (timeout.isZero() || timeout.isNegative() || timeout.compareTo(Duration.ofSeconds(4)) > 0)
            throw new IllegalArgumentException("place request timeout must be positive and at most 4 seconds");
    }
    @Override public String toString() { return "KakaoLocalProperties[credentials redacted]"; }
}
