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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.jeeware.cloud.lock4j.DistributedLockRegistry;
import io.github.jeeware.cloud.lock4j.LockRepository;
import io.github.jeeware.cloud.lock4j.function.WatchableThreadFactory;
import io.github.jeeware.cloud.lock4j.jdbc.JdbcLockRepository;
import io.github.jeeware.cloud.lock4j.mongo.IdentityExceptionTranslator;
import io.github.jeeware.cloud.lock4j.mongo.LockEntity;
import io.github.jeeware.cloud.lock4j.mongo.MongoLockRepository;
import io.github.jeeware.cloud.lock4j.redis.RedisLockRepository;
import io.github.jeeware.cloud.lock4j.redis.connection.RedisConnectionFactory;
import io.github.jeeware.cloud.lock4j.redis.connection.jedis.JedisConnectionFactory;
import io.github.jeeware.cloud.lock4j.redis.connection.lettuce.LettuceConnectionFactory;
import io.github.jeeware.cloud.lock4j.spring.MongoExceptionTranslator;
import io.github.jeeware.cloud.lock4j.spring.SQLExceptionTranslator;
import io.github.jeeware.cloud.lock4j.spring.redis.RedisConnectionFactoryAdapter;
import io.github.jeeware.cloud.lock4j.support.SimpleRetryer;
import io.lettuce.core.RedisURI;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.SocketUtils;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DistributedLockAutoConfigurationTest {

    final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DistributedLockAutoConfiguration.class, TaskSchedulingAutoConfiguration.class));

    @Test
    void distributedLockRegistryNotCreatedWhenLockTypeIsNone() {
        contextRunner.withPropertyValues("cloud.lock4j.type=none")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DistributedLockRegistry.class);
                    assertThat(context).doesNotHaveBean(LockRepository.class);
                    assertThat(context).doesNotHaveBean(Supplier.class);
                });
    }

    @Test
    void distributedLockRegistryCreatedWhenLockTypeIsJdbc() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
                .withUserConfiguration(BaseConfig.class)
                .withPropertyValues("cloud.lock4j.type=jdbc")
                .run(context -> {
                    assertThat(context).hasSingleBean(DistributedLockRegistry.class);
                    assertThat(context).hasSingleBean(JdbcLockRepository.class);
                    assertThat(context).hasSingleBean(SQLExceptionTranslator.class);
                    assertThat(context).hasSingleBean(SimpleRetryer.class);
                });
    }

    @Test
    void distributedLockRegistryCreatedWhenLockTypeIsMongoWithDataMongo() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class))
                .withUserConfiguration(MongoConfig.class)
                .withPropertyValues("cloud.lock4j.type=mongo")
                .run(context -> {
                    assertThat(context).hasSingleBean(DistributedLockRegistry.class);
                    assertThat(context).hasSingleBean(MongoLockRepository.class);
                    assertThat(context).hasSingleBean(MongoExceptionTranslator.class);
                });
    }

    @Test
    @SuppressWarnings({"java:S1874", "deprecation"})
    void distributedLockRegistryCreatedWhenLockTypeIsMongoWithoutDataMongo() {
        contextRunner
                .withUserConfiguration(MongoDatabaseConfig.class)
                .withPropertyValues("cloud.lock4j.type=mongo")
                .withClassLoader(new FilteredClassLoader(MongoDatabaseFactory.class, MongoDbFactory.class,
                        org.springframework.data.mongodb.core.MongoExceptionTranslator.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(DistributedLockRegistry.class);
                    assertThat(context).hasSingleBean(MongoLockRepository.class);
                    assertThat(context).hasSingleBean(IdentityExceptionTranslator.class);
                });
    }

    @Test
    void distributedLockRegistryCreatedWhenLockTypeIsRedisWithJedisPool() {
        assertWithRedisConfig(RedisJedisPoolConfig.class, JedisConnectionFactory.class);
    }

    @Test
    void distributedLockRegistryCreatedWhenLockTypeIsRedisWithJedisCluster() {
        assertWithRedisConfig(RedisJedisClusterConfig.class, JedisConnectionFactory.class);
    }

    @Test
    void distributedLockRegistryCreatedWhenLockTypeIsRedisWithLettuceStandalone() {
        assertWithRedisConfig(RedisLettuceStandaloneConfig.class, LettuceConnectionFactory.class);
    }

    @Test
    void distributedLockRegistryCreatedWhenLockTypeIsRedisWithLettuceCluster() {
        assertWithRedisConfig(RedisLettuceClusterConfig.class, LettuceConnectionFactory.class);
    }

    @Test
    void distributedLockRegistryCreatedWhenLockTypeIsRedisWithDataRedis() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                .withUserConfiguration(RedisConfig.class)
                .withPropertyValues("cloud.lock4j.type=redis")
                .run(context -> {
                    assertThat(context).hasSingleBean(DistributedLockRegistry.class);
                    assertThat(context).hasSingleBean(RedisLockRepository.class);
                    assertThat(context).hasSingleBean(RedisConnectionFactoryAdapter.class);
                });

    }

    private void assertWithRedisConfig(Class<?> configClass, Class<? extends RedisConnectionFactory> connectionFactoryClass) {
        contextRunner
                .withUserConfiguration(configClass)
                .withPropertyValues("cloud.lock4j.type=redis")
                .withClassLoader(new FilteredClassLoader(
                        org.springframework.data.redis.connection.jedis.JedisConnectionFactory.class,
                        org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(DistributedLockRegistry.class);
                    assertThat(context).hasSingleBean(RedisLockRepository.class);
                    assertThat(context).hasSingleBean(connectionFactoryClass);
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableScheduling
    static class BaseConfig {

        static final Runnable DO_NOTHING = () -> {
        };
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RedisConfig extends BaseConfig {

        final int port() {
            return SocketUtils.findAvailableTcpPort();
        }

        @Bean
        WatchableThreadFactory threadFactory() {
            return w -> new Thread(DO_NOTHING);
        }

    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RedisJedisPoolConfig extends RedisConfig {

        @Bean
        JedisPool jedisPool() {
            return new JedisPool("localhost", port());
        }

    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RedisJedisClusterConfig extends RedisConfig {

        @Bean
        JedisCluster jedisCluster() {
            return new JedisCluster(new HostAndPort("localhost", port()));
        }

    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RedisLettuceStandaloneConfig extends RedisConfig {

        @Bean
        LettuceConnectionFactory connectionFactory() {
            return LettuceConnectionFactory.createStandalone(RedisURI.create("localhost", port()));
        }

    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RedisLettuceClusterConfig extends RedisConfig {

        @Bean
        LettuceConnectionFactory connectionFactory() {
            return LettuceConnectionFactory.createCluster(null, RedisURI.create("localhost", port()));
        }

    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MongoConfig extends BaseConfig {

        @Bean
        WatchableThreadFactory threadFactory() {
            // disable logger error
            ((Logger) LoggerFactory.getLogger("org.mongodb.driver.cluster")).setLevel(Level.OFF);
            return w -> new Thread(DO_NOTHING);
        }

    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MongoDatabaseConfig extends MongoConfig {

        @Bean
        MongoDatabase mongoDatabase() {
            MongoDatabase databaseMock = mock(MongoDatabase.class);
            @SuppressWarnings("unchecked")
            MongoCollection<LockEntity> collectionMock = mock(MongoCollection.class);
            when(databaseMock.getCodecRegistry()).thenReturn(MongoClientSettings.getDefaultCodecRegistry());
            when(databaseMock.withCodecRegistry(any())).thenReturn(databaseMock);
            when(databaseMock.getCollection(any(), eq(LockEntity.class))).thenReturn(collectionMock);
            return databaseMock;
        }

    }

}
