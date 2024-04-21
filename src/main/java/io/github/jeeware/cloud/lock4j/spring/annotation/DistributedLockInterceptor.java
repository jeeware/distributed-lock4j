/*
 * Copyright 2020-2020-2024 Hichem BOURADA and other authors.
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

package io.github.jeeware.cloud.lock4j.spring.annotation;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import io.github.jeeware.cloud.lock4j.DistributedLockRegistry;
import io.github.jeeware.cloud.lock4j.function.Invocation;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hbourada
 * @version 1.0
 */
@RequiredArgsConstructor
@Slf4j
public class DistributedLockInterceptor implements MethodInterceptor {

    public static final Class<DistributedLock> ANNOTATION_TYPE = DistributedLock.class;

    @NonNull
    private final ConfigurableBeanFactory beanFactory;

    @NonNull
    private final DistributedLockRegistry registry;

    private final ConcurrentMap<Method, LockInfo> lockInfos = new ConcurrentHashMap<>();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final LockInfo lockInfo = lockInfos.computeIfAbsent(invocation.getMethod(), m -> buildLockInfo(invocation));
        final io.github.jeeware.cloud.lock4j.DistributedLock lock = registry.getLock(lockInfo.id);
        final Object lockResult = lockInfo.apply(lock);
        // tryLock return false => skip call
        if (Boolean.FALSE.equals(lockResult)) {
            log.debug("Distributed lock [{}] not acquired => skip method {}", lockInfo.id, invocation.getMethod());
            return null;
        }

        log.debug("Distributed lock [{}] acquired", lockInfo.id);

        try {
            return invocation.proceed();
        } finally {
            lock.unlock();
            log.debug("Distributed lock [{}] released", lockInfo.id);
        }
    }

    private LockInfo buildLockInfo(MethodInvocation mi) {
        Method method = mi.getMethod();
        DistributedLock annotation = AnnotationUtils.findAnnotation(method, ANNOTATION_TYPE);

        if (annotation == null) {
            try {
                method = mi.getThis().getClass().getMethod(method.getName(), method.getParameterTypes());
                annotation = AnnotationUtils.findAnnotation(method, ANNOTATION_TYPE);
            } catch (NoSuchMethodException ignore) {
            }
        }

        if (annotation == null) {
            throw new IllegalStateException(method + " is not annotated with " + ANNOTATION_TYPE);
        }

        final String id = annotation.id().isEmpty() ? id(method) : annotation.id();
        final Long timeout;
        String timeoutAsString = annotation.timeout();

        if (!timeoutAsString.isEmpty()
                && (timeoutAsString = beanFactory.resolveEmbeddedValue(timeoutAsString)) != null) {
            timeout = Long.valueOf(timeoutAsString);
        } else {
            timeout = null;
        }

        final Invocation invocation;

        switch (annotation.mode()) {
            case LOCK:
                invocation = Invocation.of(Lock::lock);
                break;
            case LOCK_INTERRUPTIBLE:
                invocation = l -> {
                    l.lockInterruptibly();
                    return null;

                };
                break;
            case TRY_LOCK:
                if (timeout == null) {
                    invocation = io.github.jeeware.cloud.lock4j.DistributedLock::tryLock;
                } else {
                    invocation = l -> l.tryLock(timeout, TimeUnit.MILLISECONDS);
                }
                break;
            default:
                throw new IllegalStateException("Unexpected DistributedLock.mode: " + annotation.mode());
        }

        return new LockInfo(invocation, id, timeout);
    }

    private static String id(Method method) {
        return method.getDeclaringClass().getSimpleName() + '.' + method.getName();
    }

    @RequiredArgsConstructor
    static final class LockInfo {
        final Invocation invocation;
        final String id;
        final Long timeout;

        Object apply(io.github.jeeware.cloud.lock4j.DistributedLock lock) throws InterruptedException {
            return invocation.apply(lock);
        }
    }

}
