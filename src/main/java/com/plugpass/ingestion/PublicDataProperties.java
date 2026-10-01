package com.plugpass.ingestion;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("plugpass.public-data")
public record PublicDataProperties(URI endpoint, String serviceKey, int pageSize,
        String region, Duration connectTimeout, Duration responseTimeout) {
    public PublicDataProperties {
        if (endpoint == null || endpoint.getHost() == null
                || !("http".equalsIgnoreCase(endpoint.getScheme()) || "https".equalsIgnoreCase(endpoint.getScheme()))
                || endpoint.getRawQuery() != null || endpoint.getUserInfo() != null || endpoint.getFragment() != null) {
            throw new IllegalArgumentException("endpoint must be an HTTP URL without credentials, query or fragment");
        }
        if (region == null || !region.matches("[0-9]{2}")) {
            throw new IllegalArgumentException("region must be a two-digit code");
        }
        if (pageSize < 10 || pageSize > 9999) {
            throw new IllegalArgumentException("pageSize must be between 10 and 9999");
        }
        requirePositive(connectTimeout, "connectTimeout");
        requirePositive(responseTimeout, "responseTimeout");
    }
    private static void requirePositive(Duration duration, String name) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
    @Override
    public String toString() {
        return "PublicDataProperties[credentials=<redacted>]";
    }
}
