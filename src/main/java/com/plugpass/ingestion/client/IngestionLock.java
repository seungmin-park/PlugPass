package com.plugpass.ingestion.client;

import java.util.Optional;

/** Ownership covers one entire collection without holding a database transaction. */
public interface IngestionLock {
    Optional<Lease> acquire();

    interface Lease extends AutoCloseable {
        void requireOwnership();
        @Override void close();
    }
}
