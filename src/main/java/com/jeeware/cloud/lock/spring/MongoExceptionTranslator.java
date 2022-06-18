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
