package com.plugpass.station.domain;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "charger_identity", columnNames = {"provider", "station_id", "charger_number"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Charger {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long databaseId;
    @Embedded
    private ChargerId id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_fk", nullable = false, updatable = false)
    private Station station;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ChargerStatus status;
    private String rawStatus;
    @Embedded
    private ChargerDetails details;
    private Instant sourceObservedAt;
    @Column(nullable = false)
    private Instant collectedAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    @Builder
    private Charger(ChargerId id, Station station, ChargerStatus status, String rawStatus,
            ChargerDetails details, Instant sourceObservedAt, Instant collectedAt) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        validate(status, collectedAt);
        if (station == null) {
            throw new IllegalArgumentException("station must not be null");
        }
        if (!id.provider().equals(station.getProvider()) || !id.stationId().equals(station.getStationId())) {
            throw new IllegalArgumentException("charger identity must belong to station");
        }
        this.id = id;
        this.station = station;
        this.status = status;
        this.rawStatus = rawStatus;
        this.details = details;
        this.sourceObservedAt = sourceObservedAt;
        this.collectedAt = collectedAt;
        this.createdAt = collectedAt;
        this.updatedAt = collectedAt;
    }
    private static void validate(ChargerStatus status, Instant collectedAt) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (collectedAt == null) {
            throw new IllegalArgumentException("collectedAt must not be null");
        }
    }
    public boolean acceptsObservation(Instant observedAt) {
        if (sourceObservedAt == null || observedAt == null) {
            return true;
        }
        return !observedAt.isBefore(sourceObservedAt);
    }
    public void update(ChargerStatus status, String rawStatus, ChargerDetails details, Instant observedAt, Instant collectedAt) {
        validate(status, collectedAt);
        if (!acceptsObservation(observedAt)) {
            return;
        }
        this.status = status;
        this.rawStatus = rawStatus;
        this.details = details;
        this.sourceObservedAt = observedAt;
        this.collectedAt = collectedAt;
        this.updatedAt = collectedAt;
    }
}
