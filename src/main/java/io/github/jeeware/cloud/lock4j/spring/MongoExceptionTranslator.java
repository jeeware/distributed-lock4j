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

package io.github.jeeware.cloud.lock4j.spring;

import com.mongodb.MongoException;
import io.github.jeeware.cloud.lock4j.ExceptionTranslator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * {@link ExceptionTranslator} Spring Data Mongo based implementation.
 *
 * @author hbourada
 */
@RequiredArgsConstructor
public class MongoExceptionTranslator implements ExceptionTranslator<MongoException, DataAccessException> {

    @NonNull
    private final PersistenceExceptionTranslator delegate;

    /**
     * @deprecated use {@link #MongoExceptionTranslator(PersistenceExceptionTranslator)}
     */
    @Deprecated
    public MongoExceptionTranslator() {
        this(new org.springframework.data.mongodb.core.MongoExceptionTranslator());
    }

    @Override
    public DataAccessException translate(MongoException exception, Object... args) {
        return delegate.translateExceptionIfPossible(exception);
    }
}
