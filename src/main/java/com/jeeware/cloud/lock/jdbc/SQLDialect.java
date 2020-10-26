package com.jeeware.cloud.lock.jdbc;

/**
 * Locks table structure is :
 * 
 * <pre>
 * create table if not exists LOCKS (
 * 		id varchar(255) not null primary key,
 * 		state numeric(1),
 * 		locked_at bigint,
 * 		lock_heartbeat_at bigint,
 * 		unlocked_at bigint,
 * 		locked_by varchar(255)
 * )
 * </pre>
 * 
 * We use <tt>bigint</tt> to store epoch of timestamp for locked_at,
 * unlocked_at,.. columns which is optimal for performance
 * 
 * @author hbourada
 */
public interface SQLDialect {

    default String getLock() {
        return "merge into %s l using (select ? id, ? state, ? locked_at, ? locked_by, ? lock_heartbeat_at from dual) v " +
                "on (l.id = v.id) " +
                "when not matched then " +
                "   insert (id, state, locked_at, locked_by, lock_heartbeat_at) " +
                "   values (v.id, v.state, v.locked_at, v.locked_by, v.lock_heartbeat_at) " +
                "when matched then " +
                "   update set state = v.state, locked_at = v.locked_at, locked_by = v.locked_by, " +
                "   lock_heartbeat_at = v.lock_heartbeat_at where l.state = ?";
    }

    default UpsertType upsertType() {
        return UpsertType.MERGE;
    }

    default String getUpdateLockHeartbeat() {
        return "update %s set lock_heartbeat_at = ? where state = ? and locked_by = ?";
    }

    default String getUnlock() {
        return "update %s set state = ?, unlocked_at = ? where id = ?";
    }

    default String getUnlockDeadLocks() {
        return "update %s set state = ?, unlocked_at = ? where state = ? and lock_heartbeat_at < ?";
    }

    default String getFindDeadLocks() {
        return "select * from %s where state = ? and lock_heartbeat_at < ?";
    }

    enum UpsertType {
        ON_CONFLICT, MERGE, SQL_FUNCTION
    }
}
