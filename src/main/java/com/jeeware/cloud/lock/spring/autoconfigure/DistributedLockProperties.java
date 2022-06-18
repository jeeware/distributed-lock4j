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

package com.jeeware.cloud.lock.spring.autoconfigure;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.Validate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.dao.TransientDataAccessException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link ConfigurationProperties} for distributed lock settings.
 * 
 * @author hbourada
 */
@ConfigurationProperties("cloud.lock")
@Getter
@Setter
public class DistributedLockProperties {

    private Type type;

    private final Jdbc jdbc = new Jdbc();

    private final Mongo mongo = new Mongo();

    private final Redis redis = new Redis();

    private long waitInterval = 100;

    private long refreshLockInterval = 5000;

    private long deadLockTimeout = 30000;

    private int maxRetry = 3;

    @Setter(AccessLevel.NONE)
    private String instanceId = UUID.randomUUID().toString();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Class<?>[] retryableExceptions = {TransientDataAccessException.class};

    @SafeVarargs
    public final void setRetryableExceptions(Class<? extends Exception>... retryableExceptions) {
        this.retryableExceptions = Objects.requireNonNull(retryableExceptions, "retryableExceptions is null");
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Exception>[] getRetryableExceptions() {
        return (Class<? extends Exception>[]) retryableExceptions;
    }

    public void setInstanceId(String instanceId) {
        Validate.notEmpty(instanceId, "instanceId is empty");
        this.instanceId = instanceId;
    }

    public enum Type {
        JDBC, MONGO, REDIS, NONE
    }

    @Getter
    @Setter
    public static final class Jdbc {

        private static final String DEFAULT_SCHEMA_LOCATION = "classpath:com/jeeware/cloud/lock/jdbc" +
                "/schema-@@platform@@.sql";

        @NonNull
        private String tableName = "locks";

        @NonNull
        private String functionName;

        @NonNull
        private String schemaLocation = DEFAULT_SCHEMA_LOCATION;

        private boolean createSchema = true;

        private boolean scriptContinueOnError = false;

        @NonNull
        private Charset scriptCharset = UTF_8;

        @NonNull
        private String scriptSeparator = ";;";

        public String getFunctionName() {
            if (functionName == null) {
                functionName = tableName.toLowerCase() + "__get_lock";
            }
            return functionName;
        }
    }

    @Getter
    @Setter
    public static final class Mongo {

        @NonNull
        private String collectionName = "locks";

    }

    @Getter
    @Setter
    public static final class Redis {

        @NonNull
        private String lockPrefix = "lock";

    }
}
