-- this comment line should be ignored when execute script runner
CREATE TABLE IF NOT EXISTS `LOCKS` -- this comment line should be ignored when execute script runner
(
    id                VARCHAR(255) NOT NULL PRIMARY KEY,
    state             NUMERIC(1),
    locked_at         BIGINT,
    lock_heartbeat_at BIGINT,
    unlocked_at       BIGINT,
    locked_by         VARCHAR(255) /* which instance has acquired lock */
);;

SET GLOBAL log_bin_trust_function_creators = ON;; -- ignore set global

DROP FUNCTION IF EXISTS `locks__get_lock`;;

CREATE FUNCTION `locks__get_lock`(lock_id VARCHAR(255), updated_by VARCHAR(255), updated_at BIGINT,
                                unlocked_state NUMERIC(1), locked_state NUMERIC(1))
    RETURNS INT
    MODIFIES SQL DATA
BEGIN
    DECLARE modified_count INT;
    INSERT IGNORE INTO `LOCKS` (id, state, locked_at, locked_by, lock_heartbeat_at)
    VALUES (lock_id, locked_state, updated_at, updated_by, updated_at);
    SELECT ROW_COUNT() into modified_count;
    IF modified_count = 0 THEN
        UPDATE `LOCKS`
        SET state             = locked_state,
            locked_at         = updated_at,
            locked_by         = updated_by,
            lock_heartbeat_at = updated_at
        WHERE id = lock_id AND state = unlocked_state;
        SELECT ROW_COUNT() INTO modified_count;
    END IF;

    RETURN modified_count;
END;;

SET GLOBAL log_bin_trust_function_creators = OFF;;
