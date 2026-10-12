-- MySQL 8.4 LTS. Keep identity comparisons consistent with Java String equality.
CREATE TABLE station (
    database_id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(255) NOT NULL,
    station_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (database_id),
    CONSTRAINT station_identity UNIQUE (provider, station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE charger (
    database_id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(255) NOT NULL,
    station_id VARCHAR(255) NOT NULL,
    charger_number VARCHAR(255) NOT NULL,
    station_fk BIGINT NOT NULL,
    status ENUM('AVAILABLE', 'OCCUPIED', 'UNAVAILABLE', 'UNKNOWN') NOT NULL,
    raw_status VARCHAR(255),
    connector_code VARCHAR(255),
    use_time VARCHAR(255),
    limit_yn VARCHAR(255),
    limit_detail VARCHAR(255),
    note VARCHAR(255),
    source_status_changed_at_raw VARCHAR(255),
    last_charging_started_at_raw VARCHAR(255),
    last_charging_ended_at_raw VARCHAR(255),
    charging_started_at_raw VARCHAR(255),
    del_yn VARCHAR(255),
    del_detail VARCHAR(255),
    source_observed_at DATETIME(6),
    collected_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (database_id),
    CONSTRAINT charger_identity UNIQUE (provider, station_id, charger_number),
    CONSTRAINT charger_station FOREIGN KEY (station_fk) REFERENCES station (database_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE sync_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    started_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    status ENUM('RUNNING', 'SUCCESS', 'PARTIAL_FAILURE', 'FAILURE', 'SKIPPED') NOT NULL,
    processed_count BIGINT NOT NULL,
    failed_page INT,
    failed_page_count BIGINT NOT NULL,
    failure_code VARCHAR(255),
    request_count INT NOT NULL,
    retry_count INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
