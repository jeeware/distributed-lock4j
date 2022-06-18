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

package com.jeeware.cloud.lock.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

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
     *         {@code value} is specified hence lock identifier is equals to:
     *         {@code simple-class-name + '.' + method-name}
     */
    @AliasFor("id")
    String value() default "";

    /**
     * Alias for {@link #value()}
     */
    @AliasFor("value")
    String id() default "";

    Mode mode() default Mode.LOCK;

    /**
     * @return timeout duration in milliseconds or a placeholder expression
     */
    String timeout() default "";

    enum Mode {
        /**
         * Acquire lock by calling:
         * {@link com.jeeware.cloud.lock.DistributedLock#lock()}
         */
        LOCK,

        /**
         * Try to acquire lock eventually with a timeout otherwise skip method
         */
        TRY_LOCK,

        /**
         * Acquire lock by calling:
         * {@link com.jeeware.cloud.lock.DistributedLock#lockInterruptibly()}
         */
        LOCK_INTERRUPTIBLE
    }
}
