package com.plugpass.persistence;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JpaPersistenceTests {

    @Autowired
    private PersistenceProbeRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void reloadsSavedDataFromDatabaseAfterClearingPersistenceContext() {
        var id = UUID.randomUUID();
        var original = new PersistenceProbe(id, "PlugPass persistence probe");
        var saved = repository.saveAndFlush(original);
        entityManager.clear();

        var reloaded = repository.findById(id).orElseThrow();

        assertThat(reloaded).isNotSameAs(saved);
        assertThat(reloaded.label()).isEqualTo("PlugPass persistence probe");
    }
}
