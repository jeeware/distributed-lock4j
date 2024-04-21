CREATE TABLE IF NOT EXISTS @@table@@
(
    id                VARCHAR(255) NOT NULL PRIMARY KEY,
    state             NUMERIC(1),
    locked_at         BIGINT,
    lock_heartbeat_at BIGINT,
    unlocked_at       BIGINT,
    locked_by         VARCHAR(255)
);;
