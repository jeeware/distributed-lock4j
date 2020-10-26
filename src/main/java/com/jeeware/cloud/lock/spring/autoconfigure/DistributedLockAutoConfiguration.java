package com.jeeware.cloud.lock.spring.autoconfigure;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.jeeware.cloud.lock.DistributedLockRegistry;
import com.jeeware.cloud.lock.ExceptionTranslator;
import com.jeeware.cloud.lock.LockRepository;
import com.jeeware.cloud.lock.Retryer;
import com.jeeware.cloud.lock.function.WatchableThreadFactory;
import com.jeeware.cloud.lock.jdbc.JdbcLockRepository;
import com.jeeware.cloud.lock.jdbc.SQLDialects;
import com.jeeware.cloud.lock.mongo.IdentityExceptionTranslator;
import com.jeeware.cloud.lock.mongo.MongoLockRepository;
import com.jeeware.cloud.lock.support.SimpleRetryer;
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

import com.mongodb.DB;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import com.jeeware.cloud.lock.spring.MongoExceptionTranslator;
import com.jeeware.cloud.lock.spring.SQLExceptionTranslator;
import lombok.RequiredArgsConstructor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for distributed locks.
 *
 * @author hbourada
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("'${cloud.lock.type:}'.toLowerCase() != 'none'")
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
    public Supplier<Retryer> retryerSupplier() {
        return () -> new SimpleRetryer(properties.getMaxRetry(), properties.getRetryableExceptions());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "cloud.lock.type", havingValue = "jdbc", matchIfMissing = true)
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
        @ConditionalOnProperty(value = "cloud.lock.jdbc.create-schema", matchIfMissing = true)
        @Bean
        public JdbcLockRepositoryInitializer jdbcLockRepositoryInitializer(ApplicationContext context,
                DistributedLockProperties properties) {
            return new JdbcLockRepositoryInitializer(dataSource, context, properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "cloud.lock.type", havingValue = "mongo", matchIfMissing = true)
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
