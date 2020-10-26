package com.jeeware.cloud.lock.jdbc;

import java.sql.SQLException;

import com.jeeware.cloud.lock.ExceptionTranslator;

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
