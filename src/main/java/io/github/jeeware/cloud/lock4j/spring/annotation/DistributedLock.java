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

package io.github.jeeware.cloud.lock4j.spring.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation which force acquiring a distributed lock with a specific mode
 * {@link Mode} before executing method by multiple instances eventually.
 *
 * @author hbourada
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * @return lock unique identifier. If neither of {@link #id()} nor
     * {@code value} is specified hence lock identifier is equals to:
     * {@code simple-class-name + '.' + method-name}
     */
    @AliasFor("id")
    String value() default "";

    /**
     * Alias for {@link #value()}
     *
     * @return lock unique identifier as returned by {@link #value()}
     * @see #value()
     */
    @AliasFor("value")
    String id() default "";

    Mode mode() default Mode.DEFAULT;

    /**
     * @return timeout duration or a placeholder expression evaluating to a positive duration.
     * It can be an expression with a unit suffix like 10ms, 1h...etc. or simply 1000 a positive integer in milliseconds.
     * @see io.github.jeeware.cloud.lock4j.spring.converter.StringToDurationConverter.DurationUnit
     */
    String timeout() default "";

    /**
     * @return clock skew duration or a placeholder expression evaluating to a positive duration.
     * It can be an expression with a unit suffix like 10ms, 1h...etc. or simply 1000 a positive integer in milliseconds.
     * @see io.github.jeeware.cloud.lock4j.spring.converter.StringToDurationConverter.DurationUnit
     * @since 1.0.3
     */
    String clockSkew() default "";

    enum Mode {
        /**
         * Acquire lock by calling:
         * {@link io.github.jeeware.cloud.lock4j.DistributedLock#lock()}
         */
        LOCK,

        /**
         * Try to acquire lock eventually with a timeout otherwise skip method
         */
        TRY_LOCK,

        /**
         * Acquire lock by calling:
         * {@link io.github.jeeware.cloud.lock4j.DistributedLock#lockInterruptibly()}
         */
        LOCK_INTERRUPTIBLE,

        /**
         * Try to acquire lock with a clock skew especially for scheduled tasks by calling:
         * {@link io.github.jeeware.cloud.lock4j.DistributedLock#tryLockWithClockSkew(long, TimeUnit)}
         */
        TRY_LOCK_WITH_CLOCK_SKEW,

        /**
         * Determine the mode according to the annotated method signature.
         * <ul>
         *   <li>If {@link #timeout()} is not empty then mode is {@link #TRY_LOCK} with timeout</li>
         *   <li>If {@link #clockSkew()} is not empty or @{@link org.springframework.scheduling.annotation.Scheduled}
         *   is present then mode is {@link #TRY_LOCK_WITH_CLOCK_SKEW}</li>
         *   <li>If method throws an {@link InterruptedException} then mode is {@link #LOCK_INTERRUPTIBLE}</li>
         *   <li>Otherwise mode is {@link #LOCK}</li>
         * </ul>
         */
        DEFAULT
    }
}
