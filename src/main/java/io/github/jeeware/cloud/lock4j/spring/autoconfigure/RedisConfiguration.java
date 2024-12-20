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

package io.github.jeeware.cloud.lock4j.spring.autoconfigure;

import static java.time.Duration.ofMillis;

import java.util.Collection;

import io.github.jeeware.cloud.lock4j.LockRepository;
import io.github.jeeware.cloud.lock4j.function.WatchableThreadFactory;
import io.github.jeeware.cloud.lock4j.redis.RedisLockRepository;
import io.github.jeeware.cloud.lock4j.redis.connection.RedisConnectionFactory;
import io.github.jeeware.cloud.lock4j.redis.script.DefaultRedisLockScripts;
import io.github.jeeware.cloud.lock4j.redis.script.RedisLockScripts;
import io.github.jeeware.cloud.lock4j.redis.connection.jedis.JedisConnectionFactory;
import io.github.jeeware.cloud.lock4j.redis.connection.lettuce.LettuceConnectionFactory;
import io.github.jeeware.cloud.lock4j.spring.redis.RedisConnectionFactoryAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.util.Pool;

/**
 * Configuration for Redis according to discovered client driver.
 *
 * @author hbourada
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "cloud.lock4j.type", havingValue = "redis", matchIfMissing = true)
@SuppressWarnings("java:S6830")
class RedisConfiguration {

    // distinct bean name from autoconfigured bean in RedisAutoConfiguration
    static final String CONNECTION_FACTORY_BEAN_NAME = "distributedLock.redisConnectionFactory";

    @ConditionalOnMissingBean
    @Bean(initMethod = "start")
    public LockRepository lockRepository(RedisLockScripts redisLockScripts,
                                         RedisConnectionFactory connectionFactory,
                                         DistributedLockProperties properties,
                                         ObjectProvider<WatchableThreadFactory> threadFactories) {
        RedisLockRepository repository = new RedisLockRepository(redisLockScripts, connectionFactory,
                ofMillis(properties.getDeadLockTimeout()), properties.getRedis().getLockPrefix());
        WatchableThreadFactory threadFactory = threadFactories.getIfUnique();
        if (threadFactory != null) {
            repository.setThreadFactory(threadFactory);
        }

        return repository;
    }

    @ConditionalOnMissingBean
    @Bean
    public RedisLockScripts redisLockScripts(ApplicationContext context) {
        return new DefaultRedisLockScripts(context.getClassLoader());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Jedis.class)
    static class JedisConfiguration {

        @ConditionalOnMissingBean
        @ConditionalOnBean(org.springframework.data.redis.connection.jedis.JedisConnectionFactory.class)
        @Bean(CONNECTION_FACTORY_BEAN_NAME)
        public RedisConnectionFactory redisConnectionFactory(
                org.springframework.data.redis.connection.jedis.JedisConnectionFactory connectionFactory) {
            return new RedisConnectionFactoryAdapter(connectionFactory, connectionFactory.getDatabase());
        }

        @ConditionalOnMissingBean
        @Bean(CONNECTION_FACTORY_BEAN_NAME)
        public RedisConnectionFactory redisConnectionFactoryNative(ObjectProvider<Pool<Jedis>> jedisPools,
                                                                   ObjectProvider<JedisCluster> jedisClusters) {
            Pool<Jedis> jedisPool = jedisPools.getIfUnique();

            if (jedisPool != null) {
                return JedisConnectionFactory.of(jedisPool);
            }

            JedisCluster jedisCluster = jedisClusters.getIfUnique();

            return jedisCluster != null ? JedisConnectionFactory.of(jedisCluster) : null;
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedisClient.class)
    static class LettuceConfiguration {

        @ConditionalOnMissingBean
        @ConditionalOnBean(org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory.class)
        @Bean(CONNECTION_FACTORY_BEAN_NAME)
        public RedisConnectionFactory redisConnectionFactory(
                org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory connectionFactory) {
            return new RedisConnectionFactoryAdapter(connectionFactory, connectionFactory.getDatabase());
        }

        @ConditionalOnMissingBean
        @ConditionalOnBean(RedisURI.class)
        @Bean(CONNECTION_FACTORY_BEAN_NAME)
        public RedisConnectionFactory redisConnectionFactoryNative(Collection<RedisURI> redisURIs,
                                                                   ObjectProvider<ClientResources> clientResources) {
            ClientResources resources = clientResources.getIfUnique();
            // Assume standalone
            if (redisURIs.size() == 1) {
                RedisURI uri = redisURIs.iterator().next();
                return resources != null ? LettuceConnectionFactory.createStandalone(resources, uri)
                        : LettuceConnectionFactory.createStandalone(uri);
            }

            return LettuceConnectionFactory.createCluster(resources, redisURIs.toArray(new RedisURI[0]));
        }

    }
}

