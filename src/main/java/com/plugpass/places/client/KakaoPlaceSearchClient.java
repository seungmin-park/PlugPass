package com.plugpass.places.client;

import com.plugpass.places.config.KakaoLocalProperties;
import com.plugpass.places.dto.PlaceMatch;
import com.plugpass.exception.PlaceSearchException;
import com.plugpass.exception.PlaceSearchException.Failure;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class KakaoPlaceSearchClient implements PlaceSearchClient, AutoCloseable {
    private final KakaoLocalProperties properties;
    private final JsonMapper mapper;
    private final HttpClient httpClient;
    public KakaoPlaceSearchClient(KakaoLocalProperties properties, JsonMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    @Override public List<PlaceMatch> search(String query) {
        if (properties.restApiKey().isBlank()) throw new PlaceSearchException(Failure.NOT_CONFIGURED);
        Map<LocationAddress, PlaceMatch> matches = new LinkedHashMap<>();
        for (JsonNode document : documents("address", query)) {
            String address = text(document, "address_name");
            add(matches, document, address, address);
        }
        for (JsonNode document : documents("keyword", query)) {
            String roadAddress = text(document, "road_address_name");
            String address = roadAddress.isBlank() ? text(document, "address_name") : roadAddress;
            add(matches, document, text(document, "place_name"), address);
        }
        return matches.values().stream().limit(10).toList();
    }
    private void add(Map<LocationAddress, PlaceMatch> matches, JsonNode document, String name, String address) {
        try {
            PlaceMatch place = new PlaceMatch(name, address, Double.parseDouble(text(document, "y")), Double.parseDouble(text(document, "x")));
            matches.putIfAbsent(new LocationAddress(address, place.latitude(), place.longitude()), place);
        } catch (IllegalArgumentException exception) { throw new PlaceSearchException(Failure.INVALID_RESPONSE); }
    }
    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isString()) throw new PlaceSearchException(Failure.INVALID_RESPONSE);
        return value.asString();
    }
    private JsonNode documents(String kind, String query) {
        URI uri = properties.baseUrl().resolve("/v2/local/search/" + kind + ".json?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&size=10");
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(properties.timeout())
                .header("Authorization", "KakaoAK " + properties.restApiKey()).GET().build();
        HttpResponse<String> response = send(request);
        requireSuccess(response.statusCode());
        try {
            JsonNode documents = mapper.readTree(response.body()).path("documents");
            if (!documents.isArray() || documents.size() > 10) throw new PlaceSearchException(Failure.INVALID_RESPONSE);
            return documents;
        } catch (tools.jackson.core.JacksonException exception) { throw new PlaceSearchException(Failure.INVALID_RESPONSE); }
    }
    private HttpResponse<String> send(HttpRequest request) {
        try { return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); }
        catch (HttpTimeoutException exception) { throw new PlaceSearchException(Failure.TIMEOUT); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new PlaceSearchException(Failure.UPSTREAM); }
        catch (IOException exception) { throw new PlaceSearchException(Failure.UPSTREAM); }
    }
    private void requireSuccess(int status) {
        if (status == 200) return;
        if (status == 401 || status == 403) throw new PlaceSearchException(Failure.AUTHENTICATION);
        if (status == 429) throw new PlaceSearchException(Failure.RATE_LIMIT);
        throw new PlaceSearchException(Failure.UPSTREAM);
    }
    @Override public void close() { httpClient.shutdownNow(); }
    private record LocationAddress(String address, double latitude, double longitude) { }
}
