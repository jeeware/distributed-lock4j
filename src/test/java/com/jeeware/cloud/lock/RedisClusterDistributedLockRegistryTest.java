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

package com.jeeware.cloud.lock;

import java.util.Arrays;

import com.jeeware.cloud.lock.redis.RedisLockRepository;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.SocketUtils;

import com.google.common.base.Suppliers;

import redis.embedded.RedisCluster;

/**
 * Tests for {@link DistributedLockRegistry} with
 * {@link RedisLockRepository}
 *
 * @author hbourada
 * @version 1.0
 */
@DataRedisTest(properties = "cloud.lock.type=redis", excludeAutoConfiguration = RedisRepositoriesAutoConfiguration.class)
@ContextConfiguration(classes = RedisClusterDistributedLockRegistryTest.Config.class)
@ActiveProfiles("rediscluster")
@Disabled
class RedisClusterDistributedLockRegistryTest extends DistributedLockRegistryTest {

    @DynamicPropertySource
    static void redisNodesPorts(DynamicPropertyRegistry registry) {
        registry.add("master1.port", Suppliers.memoize(SocketUtils::findAvailableTcpPort));
        registry.add("replica1.port", Suppliers.memoize(SocketUtils::findAvailableTcpPort));
        registry.add("master2.port", Suppliers.memoize(SocketUtils::findAvailableTcpPort));
        registry.add("replica2.port", Suppliers.memoize(SocketUtils::findAvailableTcpPort));
    }

    @Configuration(proxyBeanMethods = false)
    static class Config {

        @Bean(initMethod = "start", destroyMethod = "stop")
        RedisCluster redisCluster(@Value("${master1.port}") int m1Port, @Value("${replica1.port}") int r1Port,
                @Value("${master2.port}") int m2Port, @Value("${replica2.port}") int r2Port) {
            return RedisCluster.builder()
                    .sentinelCount(0)
                    .serverPorts(Arrays.asList(m1Port, r1Port))
                    .replicationGroup("master1", 1)
                    .serverPorts(Arrays.asList(m2Port, r2Port))
                    .replicationGroup("master2", 1)
                    .build();
        }

    }

}
