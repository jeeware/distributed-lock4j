/*
 * Copyright 2020-2020-2024 Hichem BOURADA and other authors.
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

package io.github.jeeware.cloud.lock4j.redis.connection.lettuce;

import java.util.Arrays;

import io.github.jeeware.cloud.lock4j.redis.connection.RedisConnection;
import io.github.jeeware.cloud.lock4j.redis.connection.RedisConnectionFactory;
import org.apache.commons.lang3.Validate;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.api.sync.RedisScriptingCommands;
import io.lettuce.core.api.sync.RedisServerCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class LettuceConnectionFactory implements RedisConnectionFactory, AutoCloseable {

    @NonNull
    private final AbstractRedisClient redisClient;

    private final int database;

    private volatile SharedConnection<?> sharedConnection;

    public static LettuceConnectionFactory createStandalone(RedisURI redisURI) {
        return new LettuceConnectionFactory(RedisClient.create(redisURI), redisURI.getDatabase());
    }

    public static LettuceConnectionFactory createStandalone(ClientResources clientResources, RedisURI redisURI) {
        return new LettuceConnectionFactory(RedisClient.create(clientResources, redisURI), redisURI.getDatabase());
    }

    public static LettuceConnectionFactory createCluster(ClientResources clientResources, RedisURI... redisURIs) {
        return createCluster(clientResources, 0, redisURIs);
    }

    public static LettuceConnectionFactory createCluster(ClientResources clientResources, int database, RedisURI... redisURIs) {
        Validate.notEmpty(redisURIs, "redisURIs is empty");
        // Ensure Redis servers have the given database number
        for (RedisURI redisURI : redisURIs) {
            if (redisURI.getDatabase() != database) {
                redisURI.setDatabase(database);
            }
        }

        final RedisClusterClient redisClient;

        if (clientResources != null) {
            redisClient = RedisClusterClient.create(clientResources, Arrays.asList(redisURIs));
        } else {
            redisClient = RedisClusterClient.create(Arrays.asList(redisURIs));
        }

        return new LettuceConnectionFactory(redisClient, database);
    }

    @Override
    public RedisConnection getConnection() {
        return new LettuceConnection(this);
    }

    @Override
    public boolean isRedisCluster() {
        return redisClient instanceof RedisClusterClient;
    }

    protected StatefulRedisPubSubConnection<String, String> getPubSubConnection() {
        if (redisClient instanceof RedisClient) {
            return ((RedisClient) redisClient).connectPubSub();
        }
        return ((RedisClusterClient) redisClient).connectPubSub();
    }

    protected SharedConnection<?> getSharedConnection() {
        if (sharedConnection == null) {
            synchronized (this) {
                if (sharedConnection == null) {
                    if (redisClient instanceof RedisClient) {
                        sharedConnection = new StandaloneSharedConnection(((RedisClient) redisClient).connect());
                    } else {
                        sharedConnection = new ClusterSharedConnection(((RedisClusterClient) redisClient).connect());
                    }
                }
            }
        }
        return sharedConnection;
    }

    protected final int getDatabase() {
        return database;
    }

    @Override
    public void close() {
        if (sharedConnection != null) {
            sharedConnection.close();
        }
        redisClient.shutdown();
    }

    interface SharedConnection<C extends RedisServerCommands<String, String> & RedisScriptingCommands<String, String>>
            extends AutoCloseable {

        C redisCommands();

        @Override
        void close();
    }

    @RequiredArgsConstructor
    static final class StandaloneSharedConnection implements SharedConnection<RedisCommands<String, String>> {

        final StatefulRedisConnection<String, String> connection;

        @Override
        public RedisCommands<String, String> redisCommands() {
            return connection.sync();
        }

        @Override
        public void close() {
            connection.close();
        }
    }

    @RequiredArgsConstructor
    static final class ClusterSharedConnection implements SharedConnection<RedisAdvancedClusterCommands<String, String>> {

        final StatefulRedisClusterConnection<String, String> connection;

        @Override
        public RedisAdvancedClusterCommands<String, String> redisCommands() {
            return connection.sync();
        }

        @Override
        public void close() {
            connection.close();
        }
    }
}
