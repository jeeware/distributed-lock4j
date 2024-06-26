/*
 * Copyright 2020-2024 Hichem BOURADA and other authors.
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

import io.github.jeeware.cloud.lock4j.jdbc.JdbcLockRepository;
import io.github.jeeware.cloud.lock4j.jdbc.SQLDialect;
import io.github.jeeware.cloud.lock4j.jdbc.SQLDialects;
import io.github.jeeware.cloud.lock4j.util.Utils;
import org.apache.commons.lang3.Validate;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Default implementation for {@link SqlDatabaseInitializer} creates tables and functions if necessary for
 * known databases as Oracle, Postgres SQL ...etc. If the dialect is not already implemented by the library
 * you can provide your own sql scripts and a default or a custom {@link SqlScriptRunner}.
 *
 * @author hbourada
 */
public class DefaultSqlDatabaseInitializer implements SqlDatabaseInitializer {

    public static final String DEFAULT_SCHEMA_ROOT_PATH = defaultSchemaRootPath();

    public static final String DEFAULT_SCHEMA_PATH = DEFAULT_SCHEMA_ROOT_PATH + "/schema-@@platform@@.sql";

    final List<SqlScript> sqlScripts;

    private final SqlScriptRunner scriptRunner;

    private static String defaultSchemaRootPath() {
        return JdbcLockRepository.class.getPackage().getName().replace('.', '/');
    }

    public DefaultSqlDatabaseInitializer(DataSource dataSource, SQLDialect dialect, Map<String, String> variables) {
        this(defaultSqlScriptRunner(dataSource, variables), defaultSqlScripts(dialect));
    }

    public DefaultSqlDatabaseInitializer(SqlScriptRunner scriptRunner, List<SqlScript> sqlScripts) {
        this.scriptRunner = Validate.notNull(scriptRunner, "scriptRunner is null");
        this.sqlScripts = Validate.notEmpty(sqlScripts, "sqlScripts is null or empty");
    }

    private static SqlScriptRunner defaultSqlScriptRunner(DataSource dataSource, Map<String, String> variables) {
        return DefaultSqlScriptRunner.builder()
                .dataSource(dataSource)
                .context(new SqlScriptRunner.Context.Default(variables))
                .build();
    }

    private static List<SqlScript> defaultSqlScripts(SQLDialect dialect) {
        SQLDialects providedDialect = Arrays.stream(SQLDialects.values())
                .filter(dialect::equals)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Default script for SQLDialect [" + dialect
                        + "] not implemented. You must provide your own sql scripts."));
        ClassLoader classLoader = Utils.defaultClassLoader();
        SqlScriptRunner.Context.Default context = new SqlScriptRunner.Context.Default();
        String schemaPath = context.with("platform", providedDialect.name().toLowerCase()).resolve(DEFAULT_SCHEMA_PATH);
        URL schemaResource = classLoader.getResource(schemaPath);
        if (schemaResource == null) {
            schemaPath = context.with("platform", "all").resolve(DEFAULT_SCHEMA_PATH);
            schemaResource = classLoader.getResource(schemaPath);
        }
        assert schemaResource != null;
        try (InputStream resourceInputStream = schemaResource.openStream()) {
            DefaultSqlScript sqlScript = new DefaultSqlScript(schemaPath, resourceInputStream, StandardCharsets.UTF_8);
            return Collections.singletonList(sqlScript);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void initializeSchemas() {
        scriptRunner.runScripts(sqlScripts);
    }

}
