package com.plugpass.ingestion.client;

import com.plugpass.ingestion.config.PublicDataProperties;
import com.plugpass.ingestion.dto.StationPage;
import com.plugpass.ingestion.dto.StationSnapshot;
import com.plugpass.ingestion.dto.StatusPage;
import com.plugpass.ingestion.dto.ChargerStatusSnapshot;
import com.plugpass.ingestion.dto.response.PublicStatusResponse;
import com.plugpass.ingestion.dto.response.PublicDataResponse;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;

public final class DefaultPublicDataClient implements PublicDataClient, PublicStatusClient {
    private final PublicDataProperties properties;
    private final Clock clock;
    private final HttpClient httpClient;
    private final PublicDataXmlParser xmlParser = new PublicDataXmlParser();
    private final ProviderStatusMapper statusMapper = new ProviderStatusMapper();

    public DefaultPublicDataClient(PublicDataProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }
    @Override
    public StationPage fetchPage(int page) { return fetchPage(page,properties.responseTimeout()); }
    @Override
    public StationPage fetchPage(int page, Duration remaining) {
        HttpResponse<byte[]> response = requestPage(page, remaining, properties.endpoint(), "");
        Instant collectedAt = clock.instant();
        PublicDataResponse providerResponse = xmlParser.parse(response.body(), properties.pageSize());
        if (providerResponse.pageNumber() != page || (providerResponse.reportedRowCount() != properties.pageSize()
                && providerResponse.reportedRowCount() != providerResponse.items().size())) {
            throw new PublicDataException(PublicDataFailure.CONTRACT);
        }
        try {
            List<StationSnapshot> snapshots = providerResponse.items().stream().map(item -> item.toSnapshot(statusMapper.map(item.rawStatus()), collectedAt)).toList();
            return new StationPage(page, properties.pageSize(), providerResponse.totalCount(), snapshots);
        } catch (IllegalArgumentException exception) {
            throw new PublicDataException(PublicDataFailure.CONTRACT);
        }
    }
    @Override
    public StatusPage fetchStatusPage(int page, Duration remaining) {
        HttpResponse<byte[]> response = requestPage(page, remaining, properties.endpoint().resolve("getChargerStatus"), "&period=10");
        PublicStatusResponse parsed = xmlParser.parseStatus(response.body(), properties.pageSize());
        if (parsed.pageNumber() != page || (parsed.reportedRowCount() != properties.pageSize()
                && parsed.reportedRowCount() != parsed.items().size())) {
            throw new PublicDataException(PublicDataFailure.CONTRACT);
        }
        Instant collectedAt = clock.instant();
        try {
            List<ChargerStatusSnapshot> snapshots = parsed.items().stream()
                    .map(item -> item.toSnapshot(statusMapper.map(item.rawStatus()), collectedAt)).toList();
            return new StatusPage(page, properties.pageSize(), parsed.totalCount(), snapshots);
        } catch (IllegalArgumentException failure) { throw new PublicDataException(PublicDataFailure.CONTRACT); }
    }

    private HttpResponse<byte[]> requestPage(int page, Duration remaining, URI endpoint, String extraParameters) {
        if (remaining == null || remaining.isZero() || remaining.isNegative()) { throw new IllegalArgumentException("remaining must be positive"); }
        Duration timeout = remaining.compareTo(properties.responseTimeout()) < 0 ? remaining : properties.responseTimeout();
        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1");
        }
        if (properties.serviceKey() == null || properties.serviceKey().isBlank()) {
            throw new PublicDataException(PublicDataFailure.AUTHENTICATION);
        }
        HttpRequest request = HttpRequest.newBuilder(requestUri(endpoint, page, extraParameters)).timeout(timeout).GET().build();
        HttpResponse<byte[]> response = sendRequest(request,timeout);
        requireHttpSuccess(response);
        return response;
    }

    private URI requestUri(URI endpoint, int page, String extraParameters) {
        return URI.create(endpoint + "?serviceKey=" + URLEncoder.encode(properties.serviceKey(), StandardCharsets.UTF_8)
                + "&pageNo=" + page + "&numOfRows=" + properties.pageSize()
                + ("ALL".equals(properties.region()) ? "" : "&zcode=" + properties.region()) + "&dataType=XML" + extraParameters);
    }
    private HttpResponse<byte[]> sendRequest(HttpRequest request, Duration timeout) {
        CompletableFuture<HttpResponse<byte[]>> responseFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
        try {
            return responseFuture.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException exception) {
            responseFuture.cancel(true);
            throw new PublicDataException(PublicDataFailure.TIMEOUT);
        } catch (InterruptedException exception) {
            responseFuture.cancel(true);
            Thread.currentThread().interrupt();
            throw new PublicDataException(PublicDataFailure.TRANSPORT);
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof HttpTimeoutException) {
                throw new PublicDataException(PublicDataFailure.TIMEOUT);
            }
            throw new PublicDataException(PublicDataFailure.TRANSPORT);
        }
    }
    private void requireHttpSuccess(HttpResponse<byte[]> response) {
        int status = response.statusCode();
        if (status == 200) {
            return;
        }
        if (status == 401 || status == 403) {
            throw new PublicDataException(PublicDataFailure.AUTHENTICATION);
        }
        if (status == 429) {
            throw new PublicDataException(PublicDataFailure.RATE_LIMIT,retryAfter(response));
        }
        if (status >= 500 && status <= 599) {
            throw new PublicDataException(PublicDataFailure.SERVER,retryAfter(response));
        }
        throw new PublicDataException(PublicDataFailure.CONTRACT);
    }
    private Duration retryAfter(HttpResponse<byte[]> response) {
        String header = response.headers().firstValue("Retry-After").orElse(null);
        if (header == null) { return null; }
        try {
            if (header.matches("[0-9]+")) { return Duration.ofSeconds(Long.parseLong(header)); }
            Instant retryAt = ZonedDateTime.parse(header,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            Duration delay = Duration.between(clock.instant(),retryAt);
            return delay.isNegative() ? Duration.ZERO : delay;
        } catch (DateTimeException | IllegalArgumentException invalidHeader) { return null; }
    }
    @Override
    public void close() {
        httpClient.shutdownNow();
    }
}
