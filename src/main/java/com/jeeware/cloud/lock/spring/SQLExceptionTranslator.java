package com.jeeware.cloud.lock.spring;

import java.sql.SQLException;

import com.jeeware.cloud.lock.ExceptionTranslator;
import org.springframework.dao.DataAccessException;

import lombok.RequiredArgsConstructor;

/**
 * {@link ExceptionTranslator} Spring JDBC based implementation.
 * 
 * @author hbourada
 */
@RequiredArgsConstructor
public class SQLExceptionTranslator implements ExceptionTranslator<SQLException, DataAccessException> {

    private final org.springframework.jdbc.support.SQLExceptionTranslator delegate;

    @Override
    public DataAccessException translate(SQLException exception, Object... args) {
        final String task = args.length > 0 ? (String) args[0] : "";
        final String sql = args.length > 1 ? (String) args[1] : "";
        return delegate.translate(task, sql, exception);
    }
}
