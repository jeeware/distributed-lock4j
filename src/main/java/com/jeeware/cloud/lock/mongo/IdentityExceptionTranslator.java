package com.jeeware.cloud.lock.mongo;

import com.mongodb.MongoException;

import com.jeeware.cloud.lock.ExceptionTranslator;

/**
 * Identity {@link ExceptionTranslator} returning the provided
 * {@link MongoException}
 */
public final class IdentityExceptionTranslator implements ExceptionTranslator<MongoException, MongoException> {

    @Override
    public MongoException translate(MongoException exception, Object... args) {
        return exception;
    }
}
