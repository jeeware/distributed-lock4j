package com.jeeware.cloud.lock.jdbc;

public enum SQLDialects implements SQLDialect {

    H2, //
    ORACLE, //
    POSTGRESQL {
        @Override
        public String getLock() {
            return "insert into %s as l (id, state, locked_at, locked_by, lock_heartbeat_at) values (?, ?, ?, ?, ?) " +
                    "on conflict (id) do " +
                    "update set state = ?, locked_at = ?, locked_by = ?, lock_heartbeat_at = ? where l.state = ?";
        }

        @Override
        public UpsertType upsertType() {
            return UpsertType.ON_CONFLICT;
        }
    },
    HSQLDB {
        @Override
        public String getLock() {
            return "merge into %s l using (values (?, ?, ?, ?, ?)) v (id, state, locked_at, locked_by, lock_heartbeat_at) " +
                    "on l.id = v.id " +
                    "when not matched then " +
                    "   insert (id, state, locked_at, locked_by, lock_heartbeat_at) " +
                    "   values (v.id, v.state, v.locked_at, v.locked_by, v.lock_heartbeat_at) " +
                    "when matched and l.state = ? then " +
                    "   update set state = v.state, locked_at = v.locked_at, locked_by = v.locked_by, " +
                    "   lock_heartbeat_at = v.lock_heartbeat_at";
        }
    },
    MYSQL {
        @Override
        public String getLock() {
            return "{? = call %s (?, ?, ?, ?, ?)}";
        }

        @Override
        public UpsertType upsertType() {
            return UpsertType.SQL_FUNCTION;
        }
    }
}
