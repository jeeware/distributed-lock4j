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

package io.github.jeeware.cloud.lock4j;

import io.github.jeeware.cloud.lock4j.redis.RedisLockRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.SocketUtils;
import org.springframework.util.function.SingletonSupplier;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * Tests for {@link DistributedLockRegistry} with
 * {@link RedisLockRepository} for standalone
 * Redis server
 *
 * @author hbourada
 * @version 1.0
 */
@DataRedisTest(properties = "cloud.lock4j.type=redis", excludeAutoConfiguration = RedisRepositoriesAutoConfiguration.class)
@ContextConfiguration(classes = RedisStandaloneDistributedLockRegistryTest.Config.class)
class RedisStandaloneDistributedLockRegistryTest extends DistributedLockRegistryTest {

    @DynamicPropertySource
    static void redisPort(DynamicPropertyRegistry registry) {
        // memoize to return same available port for property
        registry.add("spring.redis.port", SingletonSupplier.of(SocketUtils::findAvailableTcpPort));
    }

    @Configuration(proxyBeanMethods = false)
    static class Config {

        @Bean(initMethod = "start", destroyMethod = "stop")
        RedisServer redisServer(@Value("${spring.redis.port}") int port) throws IOException {
            return new RedisServer(port);
        }

    }

}
