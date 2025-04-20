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

import com.mongodb.DB;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import io.github.jeeware.cloud.lock4j.BackoffStrategy;
import io.github.jeeware.cloud.lock4j.DistributedLockRegistry;
import io.github.jeeware.cloud.lock4j.ExceptionTranslator;
import io.github.jeeware.cloud.lock4j.LockRepository;
import io.github.jeeware.cloud.lock4j.Retryer;
import io.github.jeeware.cloud.lock4j.function.WatchableThreadFactory;
import io.github.jeeware.cloud.lock4j.jdbc.JdbcLockRepository;
import io.github.jeeware.cloud.lock4j.jdbc.SQLDialects;
import io.github.jeeware.cloud.lock4j.mongo.IdentityExceptionTranslator;
import io.github.jeeware.cloud.lock4j.mongo.MongoLockRepository;
import io.github.jeeware.cloud.lock4j.spring.MongoExceptionTranslator;
import io.github.jeeware.cloud.lock4j.spring.SQLExceptionTranslator;
import io.github.jeeware.cloud.lock4j.support.RandomBackoffStrategy;
import io.github.jeeware.cloud.lock4j.support.SimpleRetryer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for distributed locks.
 *
 * @author hbourada
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("'${cloud.lock4j.type:}'.toLowerCase() != 'none'")
@EnableConfigurationProperties(DistributedLockProperties.class)
@Import({RedisConfiguration.class})
@AutoConfigureAfter({DataSourceAutoConfiguration.class, MongoDataAutoConfiguration.class, RedisAutoConfiguration.class})
@RequiredArgsConstructor
public class DistributedLockAutoConfiguration {

    private final DistributedLockProperties properties;

    @ConditionalOnMissingBean
    @Bean
    public DistributedLockRegistry distributedLockRegistry(LockRepository lockRepository,
                                                           ObjectProvider<ScheduledExecutorService> executorServices,
                                                           ObjectProvider<ThreadPoolTaskScheduler> taskSchedulers,
                                                           Supplier<Retryer> retryerSupplier) {
        ScheduledExecutorService scheduler = executorServices.getIfUnique();

        if (scheduler == null) {
            final ThreadPoolTaskScheduler taskScheduler = taskSchedulers.getIfUnique();
            if (taskScheduler != null) {
                scheduler = taskScheduler.getScheduledExecutor();
            }
        }

        final DistributedLockRegistry registry = new DistributedLockRegistry(lockRepository,
                scheduler, retryerSupplier);
        registry.setInstanceId(properties.getInstanceId());
        registry.setRefreshLockInterval(properties.getRefreshLockInterval());
        registry.setDeadLockTimeout(properties.getDeadLockTimeout());
        return registry;
    }

    @ConditionalOnMissingBean
    @Bean
    public Supplier<Retryer> retryerSupplier(BackoffStrategy backoffStrategy) {
        return () -> SimpleRetryer.builder()
                .maxRetry(properties.getRetry().getMaxRetry())
                .backoffStrategy(backoffStrategy)
                .retryableExceptions(properties.getRetryableExceptions())
                .nonRetryableExceptions(properties.getNonRetryableExceptions())
                .build();
    }

    /**
     * @since 1.0.2
     */
    @ConditionalOnMissingBean
    @Bean
    public BackoffStrategy backoffStrategy() {
        return RandomBackoffStrategy.builder()
                .random(new Random())
                .minSleepDuration(properties.getRetry().getMinSleepDuration())
                .maxSleepDuration(properties.getRetry().getMaxSleepDuration())
                .build();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "cloud.lock4j.type", havingValue = "jdbc", matchIfMissing = true)
    @ConditionalOnBean(DataSource.class)
    @RequiredArgsConstructor
    static class JdbcLockRepositoryConfiguration {

        final DataSource dataSource;

        @ConditionalOnMissingBean
        @Bean
        public LockRepository lockRepository(DataSourceProperties dataSourceProperties,
                                             SQLExceptionTranslator translator,
                                             DistributedLockProperties properties) {
            final DatabaseDriver driver = DatabaseDriver.fromJdbcUrl(dataSourceProperties.determineUrl());
            final SQLDialects dialect = SQLDialects.valueOf(driver.name());
            final DistributedLockProperties.Jdbc jdbc = properties.getJdbc();
            return new JdbcLockRepository(dataSource, dialect, translator, jdbc.getTableName(), jdbc.getFunctionName());
        }

        @ConditionalOnMissingBean
        @Bean
        public SQLExceptionTranslator exceptionTranslator() {
            return new SQLExceptionTranslator(new SQLErrorCodeSQLExceptionTranslator(dataSource));
        }

        @ConditionalOnMissingBean
        @ConditionalOnProperty(value = "cloud.lock4j.jdbc.create-schema", matchIfMissing = true)
        @Bean
        public JdbcLockRepositoryInitializer jdbcLockRepositoryInitializer(ApplicationContext context,
                                                                           DistributedLockProperties properties) {
            return new JdbcLockRepositoryInitializer(dataSource, context, properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "cloud.lock4j.type", havingValue = "mongo", matchIfMissing = true)
    @ConditionalOnClass(MongoClient.class)
    static class MongoLockRepositoryConfiguration {

        @ConditionalOnMissingBean
        @Bean(initMethod = "start")
        public LockRepository lockRepository(MongoDatabase database,
                                             ExceptionTranslator<MongoException, ? extends RuntimeException> translator,
                                             DistributedLockProperties properties,
                                             ObjectProvider<WatchableThreadFactory> threadFactories) {
            MongoLockRepository repository = new MongoLockRepository(database, properties.getMongo().getCollectionName(), translator);
            WatchableThreadFactory threadFactory = threadFactories.getIfUnique();
            if (threadFactory != null) {
                repository.setThreadFactory(threadFactory);
            }

            return repository;
        }

        @ConditionalOnMissingBean
        @ConditionalOnClass(org.springframework.data.mongodb.core.MongoExceptionTranslator.class)
        @Bean
        public ExceptionTranslator<MongoException, DataAccessException> mongoExceptionTranslator() {
            return new MongoExceptionTranslator();
        }

        @ConditionalOnMissingBean
        @Bean
        public ExceptionTranslator<MongoException, MongoException> defaultExceptionTranslator() {
            return new IdentityExceptionTranslator();
        }

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnBean(MongoDatabaseFactory.class)
        static class MongoDatabaseConfiguration {

            @ConditionalOnMissingBean
            @Bean
            public MongoDatabase mongoDatabase(MongoDatabaseFactory databaseFactory) {
                return databaseFactory.getMongoDatabase();
            }
        }

        @SuppressWarnings("deprecation")
        @Configuration(proxyBeanMethods = false)
        @ConditionalOnBean({MongoDbFactory.class, com.mongodb.MongoClient.class})
        static class LegacyMongoDatabaseConfiguration {

            @ConditionalOnMissingBean
            @Bean
            public MongoDatabase mongoDatabase(MongoDbFactory dbFactory, com.mongodb.MongoClient mongoClient) {
                Method getDbMethod = ClassUtils.getMethod(MongoDbFactory.class, "getDb");
                // spring data mongo version < 2.0
                if (getDbMethod.getReturnType() == DB.class) {
                    DB db = (DB) ReflectionUtils.invokeMethod(getDbMethod, dbFactory);
                    return mongoClient.getDatabase(Objects.requireNonNull(db).getName());
                }

                return dbFactory.getDb();
            }
        }
    }

}
