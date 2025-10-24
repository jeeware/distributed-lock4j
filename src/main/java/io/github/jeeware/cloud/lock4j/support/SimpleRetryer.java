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

package io.github.jeeware.cloud.lock4j.support;

import io.github.jeeware.cloud.lock4j.BackoffStrategy;
import io.github.jeeware.cloud.lock4j.Retryer;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple thread safe {@link Retryer} with a max retry count and a set of retryable and non retryable
 * exception base types.
 *
 * @author hbourada
 */
public class SimpleRetryer implements Retryer {

    private final int maxRetry;

    private final BackoffStrategy backoffStrategy;

    private final Map<Class<?>, Boolean> exceptionTypes;

    @Builder
    private SimpleRetryer(int maxRetry,
                          BackoffStrategy backoffStrategy,
                          @Singular Collection<Class<? extends Exception>> retryableExceptions,
                          @Singular Collection<Class<? extends Exception>> nonRetryableExceptions) {
        Validate.isTrue(maxRetry > 0, "maxRetry must be greater than 0");
        Validate.noNullElements(retryableExceptions, "retryableExceptions has a null element at index=%d");
        Validate.noNullElements(nonRetryableExceptions, "nonRetryableExceptions has a null element at index=%d");
        this.maxRetry = maxRetry;
        this.backoffStrategy = Validate.notNull(backoffStrategy, "backoffStrategy must not be null");
        this.exceptionTypes = new ConcurrentHashMap<>(retryableExceptions.size() + nonRetryableExceptions.size());
        retryableExceptions.forEach(type -> this.exceptionTypes.put(type, true));
        nonRetryableExceptions.forEach(type -> this.exceptionTypes.put(type, false));
    }

    @Override
    public boolean shouldRetryFor(Exception e, Context context) {
        if (context.isTerminated()) {
            return false;
        }
        context.incrementRetryCount();
        final List<Class<?>> childTypes = new ArrayList<>();
        Class<?> currentType = e.getClass();
        boolean retry = false;

        do {
            final Boolean currentRetry = exceptionTypes.get(currentType);
            if (currentRetry != null) {
                retry = currentRetry;
                break;
            }
            childTypes.add(currentType);
            currentType = currentType.getSuperclass();
        } while (currentType != Exception.class);

        if (!childTypes.isEmpty()) {
            final boolean r = retry;
            childTypes.forEach(type -> exceptionTypes.putIfAbsent(type, r));
        }

        return retry;

    }

    @Override
    public Context createContext() {
        return new SimpleContext(maxRetry);
    }

    @Override
    public void sleep(Context context) throws InterruptedException {
        backoffStrategy.sleep(context);
    }

    @RequiredArgsConstructor
    public static final class SimpleContext implements Context {
        private final int maxRetry;
        private int retryCount;

        @Override
        public int getRetryCount() {
            return retryCount;
        }

        @Override
        public boolean isTerminated() {
            return retryCount >= maxRetry;
        }

        @Override
        public void incrementRetryCount() {
            retryCount++;
        }

    }
}
