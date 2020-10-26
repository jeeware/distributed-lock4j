package com.jeeware.cloud.lock;

import com.jeeware.cloud.lock.redis.RedisLockRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.SocketUtils;

import com.google.common.base.Suppliers;

import redis.embedded.RedisServer;

/**
 * Tests for {@link DistributedLockRegistry} with
 * {@link RedisLockRepository} for standalone
 * Redis server
 *
 * @author hbourada
 * @version 1.0
 */
@DataRedisTest(properties = "cloud.lock.type=redis", excludeAutoConfiguration = RedisRepositoriesAutoConfiguration.class)
@ContextConfiguration(classes = RedisStandaloneDistributedLockRegistryTest.Config.class)
class RedisStandaloneDistributedLockRegistryTest extends DistributedLockRegistryTest {

    @DynamicPropertySource
    static void redisPort(DynamicPropertyRegistry registry) {
        // memoize to return same available port for property
        registry.add("spring.redis.port", Suppliers.memoize(SocketUtils::findAvailableTcpPort));
    }

    @Configuration(proxyBeanMethods = false)
    static class Config {

        @Bean(initMethod = "start", destroyMethod = "stop")
        RedisServer redisServer(@Value("${spring.redis.port}") int port) {
            return new RedisServer(port);
        }

    }

}
