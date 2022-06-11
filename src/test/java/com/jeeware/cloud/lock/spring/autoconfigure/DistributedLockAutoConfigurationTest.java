package com.jeeware.cloud.lock.spring.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import com.jeeware.cloud.lock.DistributedLockRegistry;
import com.jeeware.cloud.lock.LockRepository;
import com.jeeware.cloud.lock.jdbc.JdbcLockRepository;
import com.jeeware.cloud.lock.mongo.IdentityExceptionTranslator;
import com.jeeware.cloud.lock.mongo.LockEntity;
import com.jeeware.cloud.lock.mongo.MongoLockRepository;
import com.jeeware.cloud.lock.redis.RedisLockRepository;
import com.jeeware.cloud.lock.redis.connection.RedisConnectionFactory;
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

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.jeeware.cloud.lock.function.WatchableThreadFactory;
import com.jeeware.cloud.lock.redis.connection.jedis.JedisConnectionFactory;
import com.jeeware.cloud.lock.redis.connection.lettuce.LettuceConnectionFactory;
import com.jeeware.cloud.lock.spring.MongoExceptionTranslator;
import com.jeeware.cloud.lock.spring.SQLExceptionTranslator;
import com.jeeware.cloud.lock.spring.redis.RedisConnectionFactoryAdapter;
import com.jeeware.cloud.lock.support.SimpleRetryer;
import io.lettuce.core.RedisURI;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

class DistributedLockAutoConfigurationTest {

    final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DistributedLockAutoConfiguration.class, TaskSchedulingAutoConfiguration.class));

    @Test
    void distributedLockRegistryNotCreatedWhenLockTypeIsNone() {
        contextRunner.withPropertyValues("cloud.lock.type=none")
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
                .withPropertyValues("cloud.lock.type=jdbc")
                .run(context -> {
                    assertThat(context).hasSingleBean(DistributedLockRegistry.class);
                    assertThat(context).hasSingleBean(JdbcLockRepository.class);
                    assertThat(context).hasSingleBean(SQLExceptionTranslator.class);
                    assertThat(context).getBean(Supplier.class)
                            .extracting(Supplier::get).isInstanceOf(SimpleRetryer.class);
                });
    }

    @Test
    void distributedLockRegistryCreatedWhenLockTypeIsMongoWithDataMongo() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class))
                .withUserConfiguration(MongoConfig.class)
                .withPropertyValues("cloud.lock.type=mongo")
                .run(context -> {
                    assertThat(context).hasSingleBean(DistributedLockRegistry.class);
                    assertThat(context).hasSingleBean(MongoLockRepository.class);
                    assertThat(context).hasSingleBean(MongoExceptionTranslator.class);
                });
    }

    @Test
    void distributedLockRegistryCreatedWhenLockTypeIsMongoWithoutDataMongo() {
        contextRunner
                .withUserConfiguration(MongoDatabaseConfig.class)
                .withPropertyValues("cloud.lock.type=mongo")
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
                .withPropertyValues("cloud.lock.type=redis")
                .run(context -> {
                    assertThat(context).hasSingleBean(DistributedLockRegistry.class);
                    assertThat(context).hasSingleBean(RedisLockRepository.class);
                    assertThat(context).hasSingleBean(RedisConnectionFactoryAdapter.class);
                });

    }

    private void assertWithRedisConfig(Class<?> configClass, Class<? extends RedisConnectionFactory> connectionFactoryClass) {
        contextRunner
                .withUserConfiguration(configClass)
                .withPropertyValues("cloud.lock.type=redis")
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