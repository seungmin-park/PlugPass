package com.plugpass.ingestion;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.plugpass.exception.PublicDataException;
import com.plugpass.exception.PublicDataFailure;

public final class DefaultPublicDataClient implements PublicDataClient {
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
    public StationPage fetchPage(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1");
        }
        if (properties.serviceKey() == null || properties.serviceKey().isBlank()) {
            throw new PublicDataException(PublicDataFailure.AUTHENTICATION);
        }
        HttpRequest request = HttpRequest.newBuilder(requestUri(page)).timeout(properties.responseTimeout()).GET().build();
        HttpResponse<byte[]> response = sendRequest(request);
        Instant collectedAt = clock.instant();
        requireHttpSuccess(response.statusCode());
        PublicDataResponse data = xmlParser.parse(response.body());
        if (data.pageNumber() != page || data.pageSize() != properties.pageSize()) {
            throw new PublicDataException(PublicDataFailure.CONTRACT);
        }
        try {
            List<StationSnapshot> snapshots = data.items().stream().map(item -> item.toSnapshot(statusMapper, collectedAt)).toList();
            return new StationPage(page, data.pageSize(), data.totalCount(), snapshots);
        } catch (IllegalArgumentException exception) {
            throw new PublicDataException(PublicDataFailure.CONTRACT);
        }
    }
    private URI requestUri(int page) {
        return URI.create(properties.endpoint() + "?serviceKey=" + URLEncoder.encode(properties.serviceKey(), StandardCharsets.UTF_8)
                + "&pageNo=" + page + "&numOfRows=" + properties.pageSize() + "&zcode=" + properties.region() + "&dataType=XML");
    }
    private HttpResponse<byte[]> sendRequest(HttpRequest request) {
        CompletableFuture<HttpResponse<byte[]>> responseFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
        try {
            return responseFuture.get(properties.responseTimeout().toNanos(), TimeUnit.NANOSECONDS);
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
    private void requireHttpSuccess(int status) {
        if (status == 200) {
            return;
        }
        if (status == 401 || status == 403) {
            throw new PublicDataException(PublicDataFailure.AUTHENTICATION);
        }
        if (status == 429) {
            throw new PublicDataException(PublicDataFailure.RATE_LIMIT);
        }
        if (status >= 500 && status <= 599) {
            throw new PublicDataException(PublicDataFailure.SERVER);
        }
        throw new PublicDataException(PublicDataFailure.CONTRACT);
    }
    @Override
    public void close() {
        httpClient.shutdownNow();
    }
}
