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

import java.sql.SQLException;

import io.github.jeeware.cloud.lock4j.ExceptionTranslator;

/**
 * Simple SQL {@link ExceptionTranslator} wrapping {@link SQLException} into a
 * {@link SQLRuntimeException}.
 *
 * @author hbourada
 */
public class SQLRuntimeExceptionTranslator implements ExceptionTranslator<SQLException, SQLRuntimeException> {

    @Override
    public SQLRuntimeException translate(SQLException exception, Object... args) {
        if (args.length > 0) {
            final String task = (String) args[0];
            final String sql = args.length > 1 ? (String) args[1] : null;
            final String message = task + "; " + (sql != null ? "SQL [" + sql + "]; " : "");

            return new SQLRuntimeException(message, exception);
        }

        return new SQLRuntimeException(exception);
    }
}
