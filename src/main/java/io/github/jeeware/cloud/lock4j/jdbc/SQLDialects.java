/*
 * Copyright 2020-2020-2024 Hichem BOURADA and other authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.jeeware.cloud.lock4j.jdbc;

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
