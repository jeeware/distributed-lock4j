/*
 * Copyright 2020-2026 Hichem BOURADA and other authors.
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

import io.github.jeeware.cloud.lock4j.DistributedLockRegistry;
import io.github.jeeware.cloud.lock4j.function.Invocation;
import io.github.jeeware.cloud.lock4j.spring.annotation.DistributedLock.Mode;
import io.github.jeeware.cloud.lock4j.spring.converter.StringToDurationConverter;
import io.github.jeeware.cloud.lock4j.util.Utils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static io.github.jeeware.cloud.lock4j.util.Utils.defaultValue;

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

    private final StringToDurationConverter converter = new StringToDurationConverter();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final LockInfo lockInfo = lockInfos.computeIfAbsent(invocation.getMethod(), m -> buildLockInfo(invocation));
        final io.github.jeeware.cloud.lock4j.DistributedLock lock = registry.getLock(lockInfo.id);
        final Object lockResult = lockInfo.apply(lock);
        // tryLock return false => skip call
        if (Boolean.FALSE.equals(lockResult)) {
            log.debug("Distributed lock [{}] not acquired => skip method {}", lockInfo.id, invocation.getMethod());
            return defaultValue(invocation.getMethod().getReturnType());
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

        if (annotation == null && mi.getThis() != null) {
            try {
                method = mi.getThis().getClass().getMethod(method.getName(), method.getParameterTypes());
                annotation = AnnotationUtils.findAnnotation(method, ANNOTATION_TYPE);
            } catch (NoSuchMethodException ignore) {
                // Ignore
            }
        }

        if (annotation == null) {
            throw new IllegalStateException(method + " is not annotated with " + ANNOTATION_TYPE);
        }

        final String id = annotation.id().isEmpty() ? id(method) : annotation.id();
        final Mode mode = deduceModeIfDefault(annotation, method);
        final Invocation invocation;

        switch (mode) {
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
                validateMutuallyExclusiveWith(annotation.clockSkew(), "clockSkew", annotation);
                Long timeout = convertToMillis(annotation.timeout());
                if (timeout == null) {
                    invocation = io.github.jeeware.cloud.lock4j.DistributedLock::tryLock;
                } else {
                    invocation = l -> l.tryLock(timeout, TimeUnit.MILLISECONDS);
                }
                break;
            case TRY_LOCK_WITH_CLOCK_SKEW:
                validateMutuallyExclusiveWith(annotation.timeout(), "timeout", annotation);
                Long clockSkew = convertToMillis(annotation.clockSkew());
                if (clockSkew == null) {
                    invocation = io.github.jeeware.cloud.lock4j.DistributedLock::tryLock;
                } else {
                    invocation = l -> l.tryLockWithClockSkew(clockSkew, TimeUnit.MILLISECONDS);
                }
                break;
            default:
                throw new IllegalStateException("Unexpected DistributedLock.mode: " + mode);
        }

        return new LockInfo(invocation, id);
    }

    private static Mode deduceModeIfDefault(DistributedLock annotation, Method method) {
        Mode mode = annotation.mode();
        if (mode != Mode.DEFAULT) {
            return mode;
        }
        if (!annotation.timeout().isEmpty()) {
            return Mode.TRY_LOCK;
        }
        if (!annotation.clockSkew().isEmpty() || AnnotationUtils.findAnnotation(method, Scheduled.class) != null) {
            return Mode.TRY_LOCK_WITH_CLOCK_SKEW;
        }
        if (Utils.contains(method.getExceptionTypes(), InterruptedException.class)) {
            return Mode.LOCK_INTERRUPTIBLE;
        }
        return Mode.LOCK;
    }

    private static void validateMutuallyExclusiveWith(String value, String attributeName, DistributedLock annotation) {
        if (!value.isEmpty()) {
            throw new IllegalArgumentException(attributeName + " attribute must not be specified in " + annotation);
        }
    }

    private static String id(Method method) {
        return method.getDeclaringClass().getSimpleName() + '.' + method.getName();
    }

    private Long convertToMillis(String value) {
        String resolved = beanFactory.resolveEmbeddedValue(value);
        Duration duration = resolved != null ? converter.convert(resolved) : null;
        return duration != null ? duration.toMillis() : null;
    }

    @RequiredArgsConstructor
    static final class LockInfo {
        final Invocation invocation;
        final String id;

        Object apply(io.github.jeeware.cloud.lock4j.DistributedLock lock) throws InterruptedException {
            return invocation.apply(lock);
        }
    }

}
