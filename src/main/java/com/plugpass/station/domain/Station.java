package com.plugpass.station.domain;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "station_identity", columnNames = {"provider", "station_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Station {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long databaseId;
    @Column(nullable = false, updatable = false)
    private String provider;
    @Column(name = "station_id", nullable = false, updatable = false)
    private String stationId;
    @Column(nullable = false)
    private String name;
    @Embedded
    private GeoPoint location;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    @Builder
    private Station(String provider, String stationId, String name, GeoPoint location, Instant createdAt) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        if (stationId == null || stationId.isBlank()) {
            throw new IllegalArgumentException("stationId must not be blank");
        }
        validate(name, location, createdAt);
        this.provider = provider;
        this.stationId = stationId;
        this.name = name;
        this.location = location;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }
    private static void validate(String name, GeoPoint location, Instant changedAt) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (location == null) {
            throw new IllegalArgumentException("location must not be null");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt must not be null");
        }
    }
    public void update(String name, GeoPoint location, Instant changedAt) {
        validate(name, location, changedAt);
        this.name = name;
        this.location = location;
        this.updatedAt = changedAt;
    }
}
