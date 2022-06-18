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
