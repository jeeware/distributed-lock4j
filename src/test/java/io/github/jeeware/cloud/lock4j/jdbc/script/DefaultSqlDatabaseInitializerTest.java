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

package io.github.jeeware.cloud.lock4j.jdbc.script;

import com.google.common.collect.ImmutableMap;
import io.github.jeeware.cloud.lock4j.jdbc.SQLDialects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultSqlDatabaseInitializerTest {

    @Mock
    DataSource dataSource;

    @Mock
    Connection connection;

    @Mock
    Statement statement;

    Map<String, String> variables = ImmutableMap.of("table", "LOCKS", "function", "locks__get_lock");

    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
    }

    @Test
    void initializeSchemas_with_oracle_dialect() {
        DefaultSqlDatabaseInitializer databaseInitializer = new DefaultSqlDatabaseInitializer(dataSource, SQLDialects.ORACLE, variables);

        databaseInitializer.initializeSchemas();

        assertThat(databaseInitializer.sqlScripts).hasSize(1)
                .first()
                .extracting(SqlScript::getPath).asString()
                .endsWith("schema-oracle.sql");
    }

    @Test
    void initializeSchemas_with_mysql_dialect() {
        DefaultSqlDatabaseInitializer databaseInitializer = new DefaultSqlDatabaseInitializer(dataSource, SQLDialects.MYSQL, variables);

        databaseInitializer.initializeSchemas();

        assertThat(databaseInitializer.sqlScripts).hasSize(1)
                .first()
                .extracting(SqlScript::getPath).asString()
                .endsWith("schema-mysql.sql");
    }

    @Test
    void initializeSchemas_with_other_dialects() {
        EnumSet.complementOf(EnumSet.of(SQLDialects.ORACLE, SQLDialects.MYSQL)).forEach(dialect -> {
            DefaultSqlDatabaseInitializer databaseInitializer = new DefaultSqlDatabaseInitializer(dataSource, dialect, variables);

            databaseInitializer.initializeSchemas();

            assertThat(databaseInitializer.sqlScripts).hasSize(1)
                    .first()
                    .extracting(SqlScript::getPath).asString()
                    .endsWith("schema-all.sql");
        });

    }
}