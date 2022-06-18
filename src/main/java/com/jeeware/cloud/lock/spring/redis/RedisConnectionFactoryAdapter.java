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

package com.jeeware.cloud.lock.spring.redis;

import com.jeeware.cloud.lock.redis.connection.RedisConnection;
import com.jeeware.cloud.lock.redis.connection.RedisConnectionFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RedisConnectionFactoryAdapter implements RedisConnectionFactory {

    @NonNull
    private final org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;

    private final int database;

    @Override
    public RedisConnection getConnection() {
        return new RedisConnectionAdapter(redisConnectionFactory.getConnection(), database);
    }

}
