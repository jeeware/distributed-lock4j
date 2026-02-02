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
import com.mongodb.MongoSocketException;
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
import io.github.jeeware.cloud.lock4j.spring.autoconfigure.DistributedLockProperties.Retry;
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
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for distributed locks.
 *
 * @author hbourada
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("'${cloud.lock4j.type:}'.toLowerCase() != 'none'")
@Import({RedisConfiguration.class})
@AutoConfigureAfter({DataSourceAutoConfiguration.class, MongoDataAutoConfiguration.class, RedisAutoConfiguration.class})
@RequiredArgsConstructor
public class DistributedLockAutoConfiguration implements AutoCloseable {

    private final DistributedLockProperties properties;
    private ScheduledExecutorService createdScheduler;

    @ConditionalOnMissingBean
    @Bean
    public DistributedLockRegistry distributedLockRegistry(LockRepository lockRepository,
                                                           ObjectProvider<ScheduledExecutorService> executorServices,
                                                           ObjectProvider<ThreadPoolTaskScheduler> taskSchedulers,
                                                           Retryer retryer) {
        ScheduledExecutorService scheduler = executorServices.getIfUnique();

        if (scheduler == null) {
            final ThreadPoolTaskScheduler taskScheduler = taskSchedulers.getIfUnique();
            if (taskScheduler != null) {
                scheduler = taskScheduler.getScheduledExecutor();
            } else {
                createdScheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler = createdScheduler;
            }
        }

        final DistributedLockRegistry registry = new DistributedLockRegistry(lockRepository,
                scheduler, retryer);
        registry.setInstanceId(properties.getInstanceId());
        registry.setRefreshLockInterval(properties.getRefreshLockInterval());
        registry.setDeadLockTimeout(properties.getDeadLockTimeout());
        return registry;
    }

    @ConditionalOnMissingBean
    @Bean
    public Retryer retryer(BackoffStrategy backoffStrategy) {
        Retry retry = properties.getRetry();
        return retry.getMaxRetry() == 0 ? Retryer.NEVER : SimpleRetryer.builder()
                .maxRetry(retry.getMaxRetry())
                .backoffStrategy(backoffStrategy)
                .trackCauses(retry.isTrackCauses())
                .retryableExceptions(retry.getRetryableExceptions())
                .nonRetryableExceptions(retry.getNonRetryableExceptions())
                .build();
    }

    /**
     * @since 1.0.2
     */
    @ConditionalOnMissingBean
    @Bean
    public BackoffStrategy backoffStrategy() {
        Retry retry = properties.getRetry();
        return retry.getMaxSleepDuration().isZero() ? BackoffStrategy.NO_BACKOFF : RandomBackoffStrategy.builder()
                .random(new Random())
                .minSleepDuration(retry.getMinSleepDuration())
                .maxSleepDuration(retry.getMaxSleepDuration())
                .build();
    }

    @Override
    public void close() throws Exception {
        if (createdScheduler != null) {
            createdScheduler.shutdown();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "cloud.lock4j.type", havingValue = "jdbc", matchIfMissing = true)
    @ConditionalOnBean(DataSource.class)
    @RequiredArgsConstructor
    static class JdbcLockRepositoryConfiguration {

        final DataSource dataSource;

        @ConditionalOnMissingBean
        @Bean
        public DistributedLockProperties distributedLockProperties() {
            return DistributedLockProperties.create(
                    r -> r.withRetryableException(TransientDataAccessException.class)
                            .withNonRetryableException(NonTransientDataAccessException.class));
        }

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
        public SQLExceptionTranslator exceptionTranslator(ObjectProvider<JdbcTemplate> jdbcTemplates) {
            org.springframework.jdbc.support.SQLExceptionTranslator translator;
            JdbcTemplate jdbcTemplate = jdbcTemplates.getIfUnique();
            if (jdbcTemplate != null) {
                translator = jdbcTemplate.getExceptionTranslator();
            } else {
                translator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
            }
            return new SQLExceptionTranslator(translator);
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

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnBean(MongoDatabaseFactory.class)
        static class MongoDatabaseConfiguration {

            @ConditionalOnMissingBean
            @Bean
            public MongoDatabase mongoDatabase(MongoDatabaseFactory databaseFactory) {
                return databaseFactory.getMongoDatabase();
            }

            @ConditionalOnMissingBean
            @Bean
            public DistributedLockProperties distributedLockProperties() {
                return DistributedLockProperties.create(
                        r -> r.withRetryableException(DataAccessResourceFailureException.class,
                                UncategorizedMongoDbException.class));
            }

            @ConditionalOnMissingBean
            @Bean
            public ExceptionTranslator<MongoException, DataAccessException> mongoExceptionTranslator(
                    MongoDatabaseFactory databaseFactory) {
                return new MongoExceptionTranslator(databaseFactory.getExceptionTranslator());
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

            @ConditionalOnMissingBean
            @Bean
            public DistributedLockProperties distributedLockProperties() {
                return DistributedLockProperties.create(
                        r -> r.withRetryableException(DataAccessResourceFailureException.class,
                                UncategorizedMongoDbException.class));
            }

            @ConditionalOnMissingBean
            @Bean
            public ExceptionTranslator<MongoException, DataAccessException> mongoExceptionTranslator(
                    MongoDbFactory dbFactory) {
                return new MongoExceptionTranslator(dbFactory.getExceptionTranslator());
            }
        }

        @SuppressWarnings("deprecation")
        @ConditionalOnMissingBean({MongoDatabaseFactory.class, MongoDbFactory.class})
        @Configuration(proxyBeanMethods = false)
        static class SpringDataMongodbAbsentConfiguration {

            @Bean
            @ConditionalOnMissingBean
            public MongoDatabase mongoDatabase(MongoClient mongoClient, MongoProperties mongoProperties) {
                return mongoClient.getDatabase(mongoProperties.getMongoClientDatabase());
            }

            @ConditionalOnMissingBean
            @Bean
            public DistributedLockProperties distributedLockProperties() {
                return DistributedLockProperties.create(
                        r -> r.withRetryableException(MongoSocketException.class, IOException.class));
            }

            @ConditionalOnMissingBean
            @Bean
            public ExceptionTranslator<MongoException, MongoException> mongoExceptionTranslator() {
                return new IdentityExceptionTranslator();
            }
        }
    }

}
