package com.plugpass.ingestion.client;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessResourceFailureException;

public final class DatabaseIngestionLock implements IngestionLock {
    private final DataSource dataSource;
    private final AtomicBoolean running = new AtomicBoolean();

    public DatabaseIngestionLock(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public Optional<Lease> acquire() {
        if (!running.compareAndSet(false, true)) { return Optional.empty(); }
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            if (!"MySQL".equals(connection.getMetaData().getDatabaseProductName())) {
                connection.close();
                return Optional.of(new LocalLease());
            }
            if (!query(connection, "SELECT GET_LOCK('plugpass.ingestion', 0)")) {
                connection.close();
                running.set(false);
                return Optional.empty();
            }
            return Optional.of(new MysqlLease(connection));
        } catch (SQLException failure) {
            closeFailedConnection(connection);
            running.set(false);
            throw new DataAccessResourceFailureException("Cannot acquire ingestion ownership");
        }
    }

    private static boolean query(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(3);
            try (ResultSet result = statement.executeQuery(sql)) {
                if (!result.next() || result.getObject(1) == null) {
                    throw new SQLException("Ownership result missing");
                }
                return result.getInt(1) == 1;
            }
        }
    }

    private static void closeFailedConnection(Connection connection) {
        if (connection == null) { return; }
        try { connection.abort(Runnable::run); } catch (SQLException ignored) { /* Original failure is retained. */ }
        try { connection.close(); } catch (SQLException ignored) { /* Already unusable; never return a held lock to the pool. */ }
    }

    private final class LocalLease implements Lease {
        @Override public void requireOwnership() { }
        @Override public void close() { running.set(false); }
    }

    private final class MysqlLease implements Lease {
        private final Connection connection;
        private MysqlLease(Connection connection) { this.connection = connection; }
        @Override
        public void requireOwnership() {
            try {
                if (!query(connection, "SELECT IS_USED_LOCK('plugpass.ingestion') = CONNECTION_ID()")) {
                    throw new SQLException("Ownership lost");
                }
            } catch (SQLException failure) {
                throw new DataAccessResourceFailureException("Ingestion ownership lost");
            }
        }
        @Override
        public void close() {
            try {
                if (!query(connection, "SELECT RELEASE_LOCK('plugpass.ingestion')")) {
                    throw new SQLException("Ownership release failed");
                }
                connection.close();
            } catch (SQLException failure) {
                closeFailedConnection(connection);
                throw new DataAccessResourceFailureException("Cannot release ingestion ownership");
            } finally { running.set(false); }
        }
    }
}
