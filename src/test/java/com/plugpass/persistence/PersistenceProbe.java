package com.plugpass.persistence;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "persistence_probe")
class PersistenceProbe {

    @Id
    private UUID id;
    private String label;

    protected PersistenceProbe() {
    }

    PersistenceProbe(UUID id, String label) {
        this.id = id;
        this.label = label;
    }

    String label() {
        return label;
    }
}
