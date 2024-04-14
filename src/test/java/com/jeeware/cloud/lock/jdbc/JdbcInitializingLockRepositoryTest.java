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

package com.jeeware.cloud.lock.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcInitializingLockRepositoryTest {

    @Mock
    DataSource dataSource;

    @Mock
    Connection connection;

    @Mock
    Statement statement;

    int statementNumber;

    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).then(i -> {
            statementNumber++;
            return true;
        });
    }


    @Test
    void new_JdbcInitializingLockRepository_should_create_schema() throws SQLException {
        JdbcInitializingLockRepository lockRepository = new JdbcInitializingLockRepository(dataSource, SQLDialects.MYSQL, "lock", "get_lock");

        lockRepository.initialize();

        assertThat(statementNumber).isEqualTo(5);
    }
}