-- this comment line should be ignored when execute script runner
CREATE TABLE IF NOT EXISTS LOCKS -- XXXX ZZZZZ
(
    id                VARCHAR(255) NOT NULL PRIMARY KEY,
    state             NUMERIC(1),
    locked_at         BIGINT,
-- this 2nd comment line also will be ignored
    lock_heartbeat_at BIGINT,
    unlocked_at       BIGINT,
    /*
     * this block comment should be ignored also
     * even if its on multiple lines...
     */
    locked_by         VARCHAR(255)
);;
