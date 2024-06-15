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

package io.github.jeeware.cloud.lock4j.spring.redis;

import io.github.jeeware.cloud.lock4j.redis.connection.RedisConnection;
import io.github.jeeware.cloud.lock4j.redis.connection.RedisConnectionFactory;
import lombok.NonNull;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.Objects;

public class RedisConnectionFactoryAdapter implements RedisConnectionFactory {

    private final org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;

    private final int database;

    private final boolean redisCluster;

    public RedisConnectionFactoryAdapter(@NonNull org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory, int database) {
        this.redisConnectionFactory = Objects.requireNonNull(redisConnectionFactory, "redisConnectionFactory is null");
        this.database = database;
        this.redisCluster = isRedisCluster(redisConnectionFactory);
    }

    private static boolean isRedisCluster(org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory) {
        if (redisConnectionFactory instanceof JedisConnectionFactory) {
            return ((JedisConnectionFactory) redisConnectionFactory).isRedisClusterAware();
        }
        if (redisConnectionFactory instanceof LettuceConnectionFactory) {
            return ((LettuceConnectionFactory) redisConnectionFactory).isClusterAware();
        }
        throw new IllegalArgumentException("Unsupported Redis connection factory type: " + redisConnectionFactory);
    }

    @Override
    public RedisConnection getConnection() {
        return new RedisConnectionAdapter(redisConnectionFactory.getConnection(), database);
    }

    @Override
    public boolean isRedisCluster() {
        return this.redisCluster;
    }

}
