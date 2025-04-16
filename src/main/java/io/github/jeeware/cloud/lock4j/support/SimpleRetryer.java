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
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple {@link Retryer} with a max retry count and a set of retryable
 * exception types base.
 *
 * @author hbourada
 */
public class SimpleRetryer implements Retryer {

    private final int maxRetry;

    private final BackoffStrategy backoffStrategy;

    private final Map<Class<?>, Boolean> exceptionTypes;

    private int retryCount;

    @SafeVarargs
    @Builder
    private SimpleRetryer(int maxRetry,
                          BackoffStrategy backoffStrategy,
                          Class<? extends Exception>... exceptionTypes) {
        Validate.isTrue(maxRetry > 0, "maxRetry must be greater than 0");
        Validate.noNullElements(exceptionTypes, "exceptionTypes has a null element at index=%d");
        this.maxRetry = maxRetry;
        this.backoffStrategy = Validate.notNull(backoffStrategy, "backoffStrategy must not be null");
        this.exceptionTypes = new IdentityHashMap<>(exceptionTypes.length);

        for (Class<?> exceptionType : exceptionTypes) {
            this.exceptionTypes.put(exceptionType, true);
        }
    }

    @Override
    public boolean shouldRetryFor(Exception e) {
        if (retryCount++ >= maxRetry) {
            return false;
        }

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
            childTypes.forEach(type -> exceptionTypes.put(type, r));
        }

        return retry;

    }

    @Override
    public void sleep() throws InterruptedException {
        backoffStrategy.sleep();
    }
}
