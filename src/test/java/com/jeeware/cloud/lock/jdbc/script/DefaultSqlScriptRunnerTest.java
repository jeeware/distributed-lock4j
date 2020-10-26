/*
 * Copyright 2022-2022 Hichem BOURADA and other authors.
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

package com.jeeware.cloud.lock.jdbc.script;

import com.jeeware.cloud.lock.util.Utils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptParseException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultSqlScriptRunnerTest {

    static final String SCRIPT_ROOT_PATH = "com/jeeware/cloud/lock/jdbc/";

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
        scriptRunner = new DefaultSqlScriptRunner(dataSource);
    }

    @Test
    void runScripts() {
        List<SqlScript> sqlScripts = Stream.of("schema-test-all.sql", "schema-test-mysql.sql")
                .map(SCRIPT_ROOT_PATH::concat)
                .map(DefaultSqlScriptRunnerTest::createSqlScript)
                .collect(Collectors.toList());

        scriptRunner.runScripts(sqlScripts);

        assertThat(statements).hasSize(8)
                .allMatch(s -> !s.startsWith("--") && !s.matches("/\\*.*?\\*/"));
    }

    @SneakyThrows
    static SqlScript createSqlScript(String path) {
        try (InputStream stream = Utils.defaultClassLoader().getResourceAsStream(path)) {
            return new DefaultSqlScript(path, stream, UTF_8);
        }
    }

    @Test
    void test() {
        List<String> statements = new ArrayList<>();
        List<SqlScript> sqlScripts = Stream.of("schema-test-all.sql", "schema-test-mysql.sql")
                .map(SCRIPT_ROOT_PATH::concat)
                .map(DefaultSqlScriptRunnerTest::createSqlScript)
                .collect(Collectors.toList());
        splitSqlScript(sqlScripts.get(1).getScriptAsString(), ";;", new String[] {"--"}, "/*", "*/", statements);

        statements.forEach(System.out::println);
    }

    private static void splitSqlScript(String script,
                                       String separator, String[] commentPrefixes, String blockCommentStartDelimiter,
                                       String blockCommentEndDelimiter, List<String> statements) throws ScriptException {

        Assert.hasText(script, "'script' must not be null or empty");
        Assert.notNull(separator, "'separator' must not be null");
        Assert.notEmpty(commentPrefixes, "'commentPrefixes' must not be null or empty");
        for (String commentPrefix : commentPrefixes) {
            Assert.hasText(commentPrefix, "'commentPrefixes' must not contain null or empty elements");
        }
        Assert.hasText(blockCommentStartDelimiter, "'blockCommentStartDelimiter' must not be null or empty");
        Assert.hasText(blockCommentEndDelimiter, "'blockCommentEndDelimiter' must not be null or empty");

        StringBuilder sb = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inEscape = false;

        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            if (inEscape) {
                inEscape = false;
                sb.append(c);
                continue;
            }
            // MySQL style escapes
            if (c == '\\') {
                inEscape = true;
                sb.append(c);
                continue;
            }
            if (!inDoubleQuote && (c == '\'')) {
                inSingleQuote = !inSingleQuote;
            }
            else if (!inSingleQuote && (c == '"')) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (!inSingleQuote && !inDoubleQuote) {
                if (script.startsWith(separator, i)) {
                    // We've reached the end of the current statement
                    if (sb.length() > 0) {
                        statements.add(sb.toString());
                        sb = new StringBuilder();
                    }
                    i += separator.length() - 1;
                    continue;
                }
                else if (startsWithAny(script, commentPrefixes, i)) {
                    // Skip over any content from the start of the comment to the EOL
                    int indexOfNextNewline = script.indexOf('\n', i);
                    if (indexOfNextNewline > i) {
                        i = indexOfNextNewline;
                        continue;
                    }
                    else {
                        // If there's no EOL, we must be at the end of the script, so stop here.
                        break;
                    }
                }
                else if (script.startsWith(blockCommentStartDelimiter, i)) {
                    // Skip over any block comments
                    int indexOfCommentEnd = script.indexOf(blockCommentEndDelimiter, i);
                    if (indexOfCommentEnd > i) {
                        i = indexOfCommentEnd + blockCommentEndDelimiter.length() - 1;
                        continue;
                    }
                    else {
                        throw new ScriptParseException(
                                "Missing block comment end delimiter: " + blockCommentEndDelimiter, null);
                    }
                }
                else if (c == ' ' || c == '\r' || c == '\n' || c == '\t') {
                    // Avoid multiple adjacent whitespace characters
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                        c = ' ';
                    }
                    else {
                        continue;
                    }
                }
            }
            sb.append(c);
        }

        if (StringUtils.hasText(sb)) {
            statements.add(sb.toString());
        }
    }

    private static boolean startsWithAny(String script, String[] prefixes, int i) {
        for (String prefix : prefixes) {
            if (script.startsWith(prefix, i)) {
                return true;
            }
        }
        return false;
    }
}