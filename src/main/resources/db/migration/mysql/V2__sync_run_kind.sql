-- Additive: old JAR inserts continue to be catalog runs during rolling deployment.
ALTER TABLE sync_run ADD COLUMN kind VARCHAR(16) NOT NULL DEFAULT 'CATALOG';
CREATE INDEX sync_run_kind_status_completed ON sync_run (kind, status, completed_at);
