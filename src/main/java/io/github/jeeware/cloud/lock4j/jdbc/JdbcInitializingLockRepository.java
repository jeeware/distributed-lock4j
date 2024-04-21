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

import io.github.jeeware.cloud.lock4j.ExceptionTranslator;
import io.github.jeeware.cloud.lock4j.jdbc.script.DefaultSqlDatabaseInitializer;
import io.github.jeeware.cloud.lock4j.jdbc.script.SqlDatabaseInitializer;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A {@link JdbcLockRepository} with an {@link #initialize()} method for creating lock database schemas.
 *
 * @author hbourada
 */
public class JdbcInitializingLockRepository extends JdbcLockRepository {

    private SqlDatabaseInitializer databaseInitializer;

    public JdbcInitializingLockRepository(DataSource dataSource, SQLDialect dialect, String tableName, String functionName) {
        this(dataSource, dialect, new SQLRuntimeExceptionTranslator(), tableName, functionName);
    }

    public JdbcInitializingLockRepository(DataSource dataSource, SQLDialect dialect,
                                          ExceptionTranslator<SQLException, ? extends RuntimeException> translator,
                                          String tableName, String functionName) {
        this(dataSource, dialect, translator, tableName, functionName, new DefaultSqlDatabaseInitializer(dataSource, dialect,
                createMap(tableName, functionName)));
    }

    public JdbcInitializingLockRepository(DataSource dataSource, SQLDialect dialect,
                                          ExceptionTranslator<SQLException, ? extends RuntimeException> translator,
                                          String tableName, String functionName, SqlDatabaseInitializer databaseInitializer) {
        super(dataSource, dialect, translator, tableName, functionName);
        this.databaseInitializer = Objects.requireNonNull(databaseInitializer, "databaseInitializer is null");
    }

    private static Map<String, String> createMap(String tableName, String functionName) {
        Map<String, String> result = new HashMap<>(2, 1);
        result.put("table", tableName);
        result.put("function", functionName);
        return result;
    }

    public final void initialize() {
        if (databaseInitializer == null) {
            throw new IllegalStateException("initialize was already called");
        }
        databaseInitializer.initializeSchemas();
        databaseInitializer = null;
    }

}
