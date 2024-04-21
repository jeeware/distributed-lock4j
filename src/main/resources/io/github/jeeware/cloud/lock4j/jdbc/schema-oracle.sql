BEGIN
EXECUTE IMMEDIATE
'CREATE TABLE @@table@@
(
    id                VARCHAR2(255) NOT NULL PRIMARY KEY,
    state             NUMBER(1),
    locked_at         NUMBER,
    lock_heartbeat_at NUMBER,
    unlocked_at       NUMBER,
    locked_by         VARCHAR2(255)
)';
EXCEPTION WHEN OTHERS THEN NULL;
END;
;;
