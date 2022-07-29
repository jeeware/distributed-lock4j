/*
 * Copyright 2020-2022 Hichem BOURADA and other authors.
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

package com.jeeware.cloud.lock.jdbc;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.Validate.notBlank;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeeware.cloud.lock.ExceptionTranslator;
import com.jeeware.cloud.lock.LockRepository;
import com.jeeware.cloud.lock.jdbc.SQLDialect.UpsertType;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;

/**
 * {@link LockRepository} implementation based on a relational database table.
 *
 * @author hbourada
 * @version 1.1
 */
public class JdbcLockRepository implements LockRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcLockRepository.class);

    private static final int UNLOCKED = 0;

    private static final int LOCKED = 1;

    private final DataSource dataSource;

    private final UpsertType upsertType;

    private final ExceptionTranslator<SQLException, ? extends RuntimeException> translator;

    private final String lockSql;

    private final String updateHeartbeatSql;

    private final String unlockSql;

    private final String unlockDeadLocksSql;

    private final String findDeadLocksSql;

    public JdbcLockRepository(DataSource dataSource, SQLDialect dialect,
            ExceptionTranslator<SQLException, ? extends RuntimeException> translator,
            String tableName, String functionName) {
        notBlank(tableName, "tableName is blank");
        this.dataSource = requireNonNull(dataSource, "dataSource is null");
        this.upsertType = requireNonNull(dialect, "dialect is null").upsertType();
        this.translator = requireNonNull(translator, "translator is null");
        this.lockSql = formatLockSql(dialect, tableName, functionName);
        this.updateHeartbeatSql = format(dialect.getUpdateLockHeartbeat(), tableName);
        this.unlockSql = format(dialect.getUnlock(), tableName);
        this.unlockDeadLocksSql = format(dialect.getUnlockDeadLocks(), tableName);
        this.findDeadLocksSql = format(dialect.getFindDeadLocks(), tableName);
    }

    private static String formatLockSql(SQLDialect dialect, String tableName, String functionName) {
        return dialect.upsertType() == UpsertType.SQL_FUNCTION
                ? format(dialect.getLock(), notBlank(functionName, "functionName is blank"))
                : format(dialect.getLock(), tableName);
    }

    @Override
    public boolean acquireLock(String lockId, String instanceId) {
        final long now = System.currentTimeMillis();

        if (upsertType == UpsertType.ON_CONFLICT) {
            return execute("acquireLock", lockSql, lockId, LOCKED, now, instanceId, now,
                    LOCKED, now, instanceId, now, UNLOCKED) == 1;
        }
        if (upsertType == UpsertType.MERGE) {
            return execute("acquireLock", lockSql, lockId, LOCKED, now, instanceId, now,
                    UNLOCKED) == 1;
        }
        // UpsertType.SQL_FUNCTION
        return executeCall(lockSql, lockId, instanceId, now, UNLOCKED, LOCKED) == 1;
    }

    @Override
    public void refreshActiveLocks(String instanceId) {
        int count = execute("refreshActiveLocks", updateHeartbeatSql, System.currentTimeMillis(),
                LOCKED, instanceId);
        if (count > 0) {
            LOGGER.debug("{} locks was refreshed for instanceId: {}", count, instanceId);
        }
    }

    @Override
    public void releaseLock(String lockId, String instanceId) {
        int count = execute("releaseLock", unlockSql, UNLOCKED, System.currentTimeMillis(), lockId);
        if (count > 0) {
            LOGGER.debug("{} lock id: {} was released for instanceId: {}", count, lockId, instanceId);
        }
    }

    @Override
    public void releaseDeadLocks(long timeoutMillis) {
        long timeoutTime = System.currentTimeMillis() - timeoutMillis;

        if (LOGGER.isDebugEnabled()) {
            final List<LockEntity> locks = executeQuery(LockEntity::from, findDeadLocksSql, LOCKED, timeoutTime);
            if (!locks.isEmpty()) {
                LOGGER.debug("{} dead locks will be released => {}", locks.size(), locks);
            }
        }

        final int count = execute("releaseDeadLocks", unlockDeadLocksSql, UNLOCKED, System.currentTimeMillis(),
                LOCKED, timeoutTime);

        if (count > 0) {
            LOGGER.info("{} locks was released after timeout: {}ms", count, timeoutMillis);
        }
    }

    private int execute(String task, String sql, Object... args) {
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            if (!autoCommit) {
                connection.setAutoCommit(true);
            }
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (int i = 0; i < args.length; i++) {
                    ps.setObject(i + 1, args[i]);
                }
                return ps.executeUpdate();
            } finally {
                if (!autoCommit) {
                    connection.setAutoCommit(false);
                }
            }
        } catch (SQLException e) {
            throw translator.translate(e, task, sql);
        }
    }

    private int executeCall(String sql, Object... args) {
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            if (!autoCommit) {
                connection.setAutoCommit(true);
            }
            try (CallableStatement cs = connection.prepareCall(sql)) {
                cs.registerOutParameter(1, Types.INTEGER);
                for (int i = 0; i < args.length; i++) {
                    cs.setObject(i + 2, args[i]);
                }
                cs.execute();
                return cs.getInt(1);
            } finally {
                if (!autoCommit) {
                    connection.setAutoCommit(false);
                }
            }
        } catch (SQLException e) {
            throw translator.translate(e, "", sql);
        }
    }

    private <T> List<T> executeQuery(Function<ResultSet, T> rowMapper, String sql, Object... args) {
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            if (!autoCommit) {
                connection.setAutoCommit(true);
            }
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (int i = 0; i < args.length; i++) {
                    ps.setObject(i + 1, args[i]);
                }
                final List<T> result = new ArrayList<>();
                try (ResultSet resultSet = ps.executeQuery()) {
                    while (resultSet.next()) {
                        result.add(rowMapper.apply(resultSet));
                    }
                }
                return result;
            } finally {
                if (!autoCommit) {
                    connection.setAutoCommit(false);
                }
            }
        } catch (SQLException e) {
            throw translator.translate(e, "", sql);
        }
    }

    @ToString
    @AllArgsConstructor
    static final class LockEntity {
        final String id;
        final int state;
        final Instant lockedAt;
        final Instant unlockedAt;
        final String lockedBy;
        final Instant lockHeartbeatAt;

        @SneakyThrows
        static LockEntity from(ResultSet rs) {
            return new LockEntity(rs.getString("id"),
                    rs.getInt("state"),
                    Instant.ofEpochMilli(rs.getLong("locked_at")),
                    Instant.ofEpochMilli(rs.getLong("unlocked_at")),
                    rs.getString("locked_by"),
                    Instant.ofEpochMilli(rs.getLong("lock_heartbeat_at")));
        }
    }
}
