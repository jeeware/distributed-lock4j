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

import io.github.jeeware.cloud.lock4j.util.Utils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.jeeware.cloud.lock4j.jdbc.script.DefaultSqlDatabaseInitializer.DEFAULT_SCHEMA_ROOT_PATH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultSqlScriptRunnerTest {

    static final String SCRIPT_ROOT_PATH = DEFAULT_SCHEMA_ROOT_PATH;

    @Mock
    DataSource dataSource;

    @Mock
    Connection connection;

    @Mock
    Statement statement;

    SqlScriptRunner scriptRunner;

    List<String> statements = new ArrayList<>();

    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(any())).then(i -> statements.add(i.getArgument(0)));
        scriptRunner = DefaultSqlScriptRunner.builder().dataSource(dataSource).build();
    }

    @Test
    void runScripts() {
        List<SqlScript> sqlScripts = Stream.of("/schema-test-all.sql", "/schema-test-mysql.sql")
                .map(SCRIPT_ROOT_PATH::concat)
                .map(DefaultSqlScriptRunnerTest::createSqlScript)
                .collect(Collectors.toList());

        scriptRunner.runScripts(sqlScripts);

        assertThat(statements).hasSize(6)
                .allMatch(s -> !s.matches("/\\*.*?\\*/|--.*"));
    }

    @SneakyThrows
    static SqlScript createSqlScript(String path) {
        try (InputStream stream = Utils.defaultClassLoader().getResourceAsStream(path)) {
            return new DefaultSqlScript(path, stream, UTF_8);
        }
    }

}