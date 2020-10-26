package com.jeeware.cloud.lock.spring;

import com.jeeware.cloud.lock.ExceptionTranslator;
import org.springframework.dao.DataAccessException;

import com.mongodb.MongoException;

/**
 * {@link ExceptionTranslator} Spring Data Mongo based implementation.
 *
 * @author hbourada
 */
public class MongoExceptionTranslator implements ExceptionTranslator<MongoException, DataAccessException> {

    private final org.springframework.data.mongodb.core.MongoExceptionTranslator delegate;

    public MongoExceptionTranslator() {
        this.delegate = new org.springframework.data.mongodb.core.MongoExceptionTranslator();
    }

    @Override
    public DataAccessException translate(MongoException exception, Object... args) {
        return delegate.translateExceptionIfPossible(exception);
    }
}
