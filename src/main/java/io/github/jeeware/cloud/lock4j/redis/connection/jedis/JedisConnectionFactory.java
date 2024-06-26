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

package io.github.jeeware.cloud.lock4j.redis.connection.jedis;

import io.github.jeeware.cloud.lock4j.redis.connection.RedisConnectionFactory;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.util.Pool;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class JedisConnectionFactory implements RedisConnectionFactory {

    public static JedisConnectionFactory of(Pool<Jedis> jedisPool) {
        return new JedisPoolConnectionFactory(jedisPool);
    }

    public static JedisConnectionFactory of(JedisCluster jedisCluster) {
        return new JedisClusterConnectionFactory(jedisCluster);
    }

    @RequiredArgsConstructor
    static final class JedisPoolConnectionFactory extends JedisConnectionFactory {

        @NonNull
        final Pool<Jedis> jedisPool;

        @Override
        public JedisConnection getConnection() {
            return new JedisConnection(new JedisCommandsImpl(jedisPool.getResource()));
        }

        @Override
        public boolean isRedisCluster() {
            return false;
        }

    }

    @RequiredArgsConstructor
    static final class JedisClusterConnectionFactory extends JedisConnectionFactory {

        @NonNull
        final JedisCluster jedisCluster;

        @Override
        public JedisConnection getConnection() {
            return new JedisConnection(new JedisClusterCommandsImpl(jedisCluster));
        }

        @Override
        public boolean isRedisCluster() {
            return true;
        }

    }

}
