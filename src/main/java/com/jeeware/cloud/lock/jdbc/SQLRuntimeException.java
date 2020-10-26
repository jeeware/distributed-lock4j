package com.jeeware.cloud.lock.jdbc;

import java.sql.SQLException;

/**
 * Wrap a {@link SQLException} into a {@link RuntimeException}.
 * 
 * @author hbourada
 */
public class SQLRuntimeException extends RuntimeException {

    public SQLRuntimeException(SQLException cause) {
        super(cause);
    }

    public SQLRuntimeException(String message, SQLException cause) {
        super(message, cause);
    }

    @Override
    public synchronized SQLException getCause() {
        return (SQLException) super.getCause();
    }
}
