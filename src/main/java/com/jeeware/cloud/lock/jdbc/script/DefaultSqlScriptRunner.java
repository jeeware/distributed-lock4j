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

package com.jeeware.cloud.lock.jdbc.script;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultSqlScriptRunner implements SqlScriptRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSqlScriptRunner.class);

    @NonNull
    private DataSource dataSource;

    @Builder.Default
    private boolean continueOnError = true;

    @NonNull
    @Builder.Default
    private String separator = ";;";

    @NonNull
    @Builder.Default
    private String commentPrefix = "--";

    @NonNull
    @Builder.Default
    private String blockCommentStartDelimiter = "/*";

    @NonNull
    @Builder.Default
    private String blockCommentEndDelimiter = "*/";

    @NonNull
    @Builder.Default
    private Context context = Context.IDENTITY;

    @Override
    public void runScripts(List<SqlScript> scripts) {
        long startTime = System.currentTimeMillis();
        int statementNumber = 0;

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (SqlScript script : scripts) {
                for (String sql : splitToSqlStatements(context.resolve(script.getScriptAsString()))) {
                    statementNumber++;
                    execute(statement, sql);
                }
            }
            long elapsedTime = System.currentTimeMillis() - startTime;
            LOGGER.debug("Executed {} statements in {} ms.", statementNumber, elapsedTime);
        } catch (SQLException e) {
            throw new SqlScriptException(e);
        }
    }

    private List<String> splitToSqlStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder sb = new StringBuilder(64);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inEscape = false;
        char[] scriptChars = script.toCharArray();

        for (int i = 0; i < scriptChars.length; i++) {
            char c = scriptChars[i];
            if (inEscape) {
                inEscape = false;
                sb.append(c);
                continue;
            }
            if (c == '\\') { // MySQL style escapes
                inEscape = true;
                sb.append(c);
                continue;
            }
            if (!inDoubleQuote && (c == '\'')) {
                inSingleQuote = !inSingleQuote;
            } else if (!inSingleQuote && (c == '"')) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (!inSingleQuote && !inDoubleQuote) {
                if (script.startsWith(separator, i)) {
                    // We've reached the end of the current statement
                    if (sb.length() > 0) {
                        statements.add(sb.toString());
                        sb = new StringBuilder(64);
                    }
                    i += separator.length();
                    continue;
                } else if (script.startsWith(commentPrefix, i)) {
                    // Skip over any content from the start of the comment to the EOL
                    int indexOfNextNewline = script.indexOf('\n', i);
                    if (indexOfNextNewline > i) {
                        i = indexOfNextNewline;
                        continue;
                    } else {
                        // If there's no EOL, we must be at the end of the script, so stop here.
                        break;
                    }
                } else if (script.startsWith(blockCommentStartDelimiter, i)) {
                    // Skip over any block comments
                    int indexOfCommentEnd = script.indexOf(blockCommentEndDelimiter, i);
                    if (indexOfCommentEnd > i) {
                        i = indexOfCommentEnd + blockCommentEndDelimiter.length() - 1;
                        continue;
                    } else {
                        throw new SqlScriptException("Missing block comment end delimiter: " + blockCommentEndDelimiter, null);
                    }
                } else if (c == ' ' || c == '\r' || c == '\n' || c == '\t') {
                    // Avoid multiple adjacent whitespace characters
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                        c = ' ';
                    } else {
                        continue;
                    }
                }
            }
            sb.append(c);
        }

        if (StringUtils.isNotBlank(sb)) {
            statements.add(sb.toString());
        }

        return statements;
    }

    private void execute(Statement statement, String sql) {
        try {
            statement.execute(sql);
            int updateCount = statement.getUpdateCount();
            LOGGER.debug("{} returned as update count for SQL: {}", updateCount, sql);
        } catch (SQLException ex) {
            if (continueOnError) {
                LOGGER.warn("Error occurred in script {}", sql, ex);
            } else {
                throw new SqlScriptException("Error occurred in script: " + sql, ex);
            }
        }
    }

}
